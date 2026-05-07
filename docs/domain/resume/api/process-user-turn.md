# API: 사용자 턴 처리 (process turn)

> Endpoint: `POST /api/interviews/{id}/follow-up` — `FollowUpService` 진입 → resume 트랙 분기 위임 (`ResumeInterviewOrchestrator.processUserTurn`)
> Action: 매 턴 모드 분기 (Playground / Interrogation), 다음 질문 생성 또는 종료. 현재 모드 유지 / 전이를 함께 결정. 종료 시점은 FE 종료 신호 (`FollowUpRequest.terminate=true`) 또는 hard timeout backstop.
> 관련 테이블: `question` (write) / `question_set` (read / write find-or-create) / `interview_plan` (read) / `resume_skeleton` (read) / `interview` (read — owner / duration)
> 관련 외부 의존: OpenAI GPT-4o-mini → Claude Haiku (intent 분석 / answer analysis / 모드별 question builder)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| path | `id` | Long | required | interviewId |
| body | `answerText` | string | required, non-empty | 사용자 답변 텍스트 |
| body | `audioFile` | bytes | required (FollowUpService 가드) | 음성 (resume 트랙은 무시 — Issue #408 A1) |
| body | `previousExchanges` | list | optional | 이전 대화 페이로드 |
| header | `Authorization` | Bearer | required | JWT |

> resume 트랙은 text-only. `FollowUpService` 진입부에서 audio empty 시 `ANSWER_TEXT_REQUIRED` 강제.

---

## 출력 (정상)

| 필드 | 타입 | 의미 |
|------|------|------|
| `questionId` | Long | 생성된 다음 질문 PK (또는 종료 시 null) |
| `questionText` | string | 다음 질문 또는 wrap-up 멘트 |
| `mode` | enum | 응답 시점 mode (`PLAYGROUND` / `INTERROGATION`) |
| `followUpExhausted` | bool | hard timeout / FE 종료 신호 / context budget 초과 시 true |

부수 효과:
- 0 또는 1건 `question` INSERT (questionType = mode 별 상수 — `RESUME_PLAYGROUND` / `RESUME_INTERROGATION`). 종료 응답 시 INSERT 0.
- `InterviewRuntimeState` mutate (Caffeine 캐시 — playgroundTurns / chainStateTracker / resumeMode).
- `TurnCompletedEvent.ofResumeTrack` 발행 (`ApplicationEventPublisher`).

## 출력 (실패)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `ANSWER_TEXT_REQUIRED` | text 비어 있음 (FollowUpService 가드) |
| 400 | `RESUME_EXCLUSIVITY_VIOLATION` / `RESUME_REQUIRED_FOR_RESUME_BASED` | 트랙/리소스 매칭 위반 (FollowUp 진입부) |
| 403 | `FORBIDDEN` | `Interview.validateOwner` 실패 |
| 404 | `PLAN_NOT_FOUND` | plan 미부착 |
| 409 | `RESUME_INTERROGATION_EXHAUSTED` | Interrogation 모든 chain 소진 (응답 통보, 모드 변경 안 함) |
| 500 | `INTERNAL_SERVER_ERROR` | runtime state 부재 (`IllegalStateException`) — 재시작 / TTL 만료 / 노드 라우팅 (Issue #408 C1) |
| 502 | `AI_*` (`CLIENT_ERROR` / `EMPTY_RESPONSE` / `PARSE_FAILED` / `RESPONSE_INVALID`) | LLM 결함 |
| 503 | `AI_SERVICE_UNAVAILABLE` | 이중 장애 |
| 504 | `AI_TIMEOUT` | 타임아웃 |
| 429 | `RATE_LIMITED` | Resilience4j |

---

## 흐름

### 1. 진입 / 권한
- `FollowUpService.generateFollowUp` 진입 (`@Transactional(propagation = NOT_SUPPORTED)`).
- `FollowUpTransactionHandler.loadFollowUpContext` 가 `Interview.validateOwner(userId)` 호출 (resume 트랙 분기 전).
- `isResumeTrack(state, interview)` 판정 → `delegateToResumeOrchestrator` 위임.
- skeleton 캐시 미스 시 `ResumeSkeletonPersister.findByInterviewId` 로 hydrate.
- plan 캐시 미스 시 `InterviewPlanPersister.findByInterviewId` 또는 fallback `ResumeInterviewPlanner.plan` 재산출.

### 2. 시계 검사 (`ClockWatcher`)
- 인터뷰 첫 호출 시 `markStart` 1회 (`Clock` bean 의존, ChronoUnit.MINUTES).
- `remainingMinutes()` = `durationMinutes - elapsed`.
- `isHardTimeoutExceeded` (식: `elapsed >= duration + hard-timeout-min(10)`) 시 hard timeout backstop → `RESUME_HARD_TIMEOUT` 응답 (`followUpExhausted=true`).
- 정상 종료 경로 = FE 가 잔여 시간 ≤ 0 도달 + 답변 완료 시점에 `terminate=true` 동봉 → BE 가 답변 분석 후 신규 질문 INSERT skip + `followUpExhausted=true` 응답.

### 3. 턴 분석 (`TurnAnalysisPipeline`)
- `IntentClassifier.classify` — 신뢰도 < 0.7 (`fallback-on-low-confidence`) 시 `forceAnswer()` 환원. 모든 예외 catch → `forceAnswer()` (사용자 흐름 차단 금지).
- intent == `ANSWER` 시 `AnswerAnalyzer.analyze` 실행 (temp 0.2, maxTokens 800).
  - L1 False Negative Guard: `claims=[] AND quality≤1 AND action!=CLARIFICATION` → 강제 `CLARIFICATION` override (INFO 로그).
- intent ≠ ANSWER 시 `AnswerAnalysis.empty(turnIndex)` 반환 (LLM 미호출).

### 4. non-ANSWER intent 분기 (`IntentDispatcher`)
- `CLARIFY_REQUEST` → `ClarifyResponseHandler`.
- `OFF_TOPIC` → `OffTopicResponseHandler` — 누적 횟수 ≥ `off-topic-consecutive-limit: 3` 시 `GiveUpResponseHandler` escalate.
- `GIVE_UP` → `GiveUpResponseHandler`.
- 미등록 intent → `IllegalStateException` (스타트업 가드 — 발생 가능 X).

### 5. 모드 전이 결정 (`ResumeModeTransitionPolicy`)
- hard timeout backstop: `isHardTimeoutExceeded(durationMinutes, remainingMinutes)` true 면 `RESUME_HARD_TIMEOUT` 응답.
- Playground → Interrogation 전이: `PlaygroundModeHandler.evaluateSwitchConditions` LLM 출력 4 boolean (`a_covered` / `b_length_ok` / `c_signal` / `d_turn_limit`) 중 ≥2 또는 강제 플래그 — Java 측 turn count 임계값 미적용 (LLM 자체 판단).
- Interrogation → Playground 역방향 전이 없음.

### 6. 분기 (`dispatchByMode`)

#### 6-A. Playground (대화 기반 탐색)
1. `PlaygroundModeHandler.handle(intent, state)` — 다음 질문 생성.
   - `ResumePlaygroundPromptBuilder` (`RESUME_PLAYGROUND_RESPONDER` callType, yml `rehearse.resume-track.*`).
   - playgroundTurns / playgroundCumulativeLength mutate.
2. switch 4조건 ≥ 2 충족 시 INTERROGATION 전이 → 같은 턴 내 `InterrogationModeHandler.handle(... null, null, ...)` 즉시 후속 호출 (사용자 답변 없이 첫 chain 질문 생성).
3. `ResumeQuestionPersister.persist(..., type=RESUME_PLAYGROUND)`.

#### 6-B. Interrogation (체인 기반 심화)
1. `ChainStateTracker.withLock(action)` — per-instance ReentrantLock 으로 세션 단일 처리.
2. `resolveNextChain` → primary → backup chains (priority 오름차순).
3. `ResumeChainInterrogatorPromptBuilder` (`RESUME_CHAIN_INTERROGATOR` callType) → LLM 의 `nextAction` (LEVEL_UP / LEVEL_STAY / CHAIN_SWITCH) 결정.
4. `applyDecision`:
   - `LEVEL_STAY` 누적 ≥ `LEVEL_STAY_MAX_TURNS=2` (hardcoded) + `level < 4` → 강제 `LEVEL_UP`.
   - `level == 4` → 강제 `CHAIN_SWITCH` (level cap 4 hardcoded).
5. `resolveNextChain.isEmpty()` 시 `RESUME_INTERROGATION_EXHAUSTED` 응답 (409) — 모드 변경 안 함.
6. `ResumeQuestionPersister.persist(..., type=RESUME_INTERROGATION)`.

#### 6-C. 종료 분기 (FE 신호 / hard timeout backstop)
1. `terminate==true`: 답변 분석 (`turnAnalysisPipeline.analyze`) 까지는 수행. 신규 question INSERT skip. `turnEventPublisher.publish` 미호출. `followUpExhausted=true / skip=true / presentToUser=false` 응답.
   - 로그: `[ResumeOrchestrator] FE-signaled terminate: interviewId={}` (INFO).
2. hard timeout backstop: 분석 후 `isHardTimeoutExceeded` true → `RESUME_HARD_TIMEOUT` 응답 (`followUpExhausted=true / skip=true / presentToUser=false`). dispatch / publish 미진입.
   - 로그: `[ResumeOrchestrator] hard timeout backstop: interviewId={}` (WARN).
3. hard timeout 이 terminate 보다 우선 — 동시 발생 시 `RESUME_HARD_TIMEOUT` 응답.

### 7. 외부 호출
- 모든 LLM 호출 `ResilientAiClient` 경유 (OpenAI primary → Claude fallback).
- Sampling: resume-track yml 공유 (`temperature 0.7, maxTokens 800`).
- Retry / parse: ingest / planner 와 동일 (worst 5회 외부 호출 + schema-hint 1회).
- `max-tokens=800` 한계로 응답 잘림 위험 (`finishReason=length`) — WARN 로그만, parser 진입 시 `PARSE_FAILED` 가능.

### 8. 이벤트 발행
- `ResumeTurnEventPublisher.publish` — `TurnCompletedEvent.ofResumeTrack(interviewId, turnIndex, userId, questionId, questionSetId, userAnswer, analysis, intent, userLevel, resumeMode, currentChainLevel, resumeSkeleton)`.
- 발행 실패 catch + WARN (턴 진행 차단 X — graceful degradation).
- feedback 도메인 listener (RubricScorer 등) 가 청취.

### 9. 응답
- 다음 질문 + 모드 + terminated 플래그 반환.

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 사용자 답변 text 빈 값 | 400 `ANSWER_TEXT_REQUIRED` (FollowUpService 진입부) |
| audio 만 보내고 text 없음 | 400 `ANSWER_TEXT_REQUIRED` (resume 트랙은 audio 자체 무시 — Issue #408 A1) |
| 다른 사용자 인터뷰 호출 | 403 `FORBIDDEN` |
| plan 미부착 인터뷰 호출 | 404 `PLAN_NOT_FOUND` |
| skeleton 캐시 미스 | DB 재조회 hydrate (정상) |
| plan 캐시 미스 | DB 재조회 → 부재 시 `ResumeInterviewPlanner.plan` 재산출 |
| Playground 4 boolean 중 ≥ 2 충족 | 같은 턴 내 INTERROGATION 으로 전이 + 즉시 첫 chain 질문 생성 |
| Interrogation 동일 chain LEVEL_STAY 2 턴 누적 + level < 4 | 강제 LEVEL_UP |
| Interrogation level == 4 LEVEL_STAY 시도 | 강제 CHAIN_SWITCH |
| Interrogation 모든 chain 소진 | 409 `RESUME_INTERROGATION_EXHAUSTED` (모드 유지) |
| FE 가 `terminate=true` 동봉 | 답변 분석 후 INSERT skip + `followUpExhausted=true` 응답 (publish 미호출) |
| elapsed ≥ duration + 10분 | hard timeout backstop, `RESUME_HARD_TIMEOUT` 응답 (`followUpExhausted=true`) |
| terminate=true + hard timeout 동시 | hard timeout 우선 — `RESUME_HARD_TIMEOUT` 응답 |
| LLM 5xx → fallback 5xx | 503 `AI_SERVICE_UNAVAILABLE` |
| LLM schema 위반 2회 | 502 `AI_PARSE_FAILED` |
| LLM 응답 question text blank | 502 `AI_RESPONSE_INVALID` |
| LLM 응답이 `ResumeFallbackQuestions.*` 와 동일 | WARN 로그 (저장은 진행) — Issue #408 A3 |
| OpenAI 4xx (non-429) | 즉시 502 `AI_CLIENT_ERROR` (fallback 진입 X) |
| `LLM finishReason=length` (max-tokens 도달) | WARN 로그 + parser 진입 시 PARSE_FAILED 가능 |
| runtime state Map 부재 (TTL 만료 / Caffeine evict / 노드 재배포) | 500 `IllegalStateException` (Issue #408 C1) |
| 동시 follow-up 호출 (같은 인터뷰) | `ChainStateTracker` per-instance ReentrantLock 직렬화. Caffeine `update` mutator atomic. PlaygroundHandler 는 락 없음 — Issue #408 |
| Caffeine eviction (LRU / TTL) | skeleton/plan 캐시 손실 → DB 재조회 복원. 단 ChainStateTracker 진행 상태는 in-memory only — 유실 (Issue #408 C2) |

---

## 상태 전이

```
PLAYGROUND ──(LLM 4 boolean ≥ 2 또는 강제)──▶ INTERROGATION
INTERROGATION ──(모든 chain 소진)──────────▶ INTERROGATION (응답 409, 모드 유지)
PLAYGROUND / INTERROGATION ──(FE terminate=true)─────▶ TERMINATED (followUpExhausted=true, INSERT skip)
PLAYGROUND / INTERROGATION ──(elapsed ≥ duration+10m)─▶ TERMINATED (RESUME_HARD_TIMEOUT)
```

- INTERROGATION → PLAYGROUND 역방향 전이 없음.
- 종료 분기는 FE `terminate=true` 또는 hard timeout backstop 단일 경로. 별도 종료 전용 모드 미존재 (FSM 2단계 단순화).

---

## 관찰성

- **로그**: `[ResumeOrchestrator]` / `[PlaygroundHandler]` / `[InterrogationHandler]` / `[ChainStateTracker]` / `[ResumeModeTransitionPolicy]`
  - 표준 포맷: `interviewId={}, intent={}, mode={}, chainId={}, level={}, action={}, turnCount={}, remainingMin={}`
  - 모든 로그에 `interviewId` 일관 포함. 사용자 답변 본문 / 이력서 본문 로깅 금지.
- **메트릭** (`AiCallMetrics`):
  - `rehearse.ai.call.duration{call.type=INTENT_CLASSIFIER|ANSWER_ANALYZER|RESUME_PLAYGROUND_RESPONDER|RESUME_CHAIN_INTERROGATOR, model, provider, cache.hit, fallback, outcome}`
  - `rehearse.ai.parse.fail.total{stage=first|second, call.type=resume.*}`
  - `rehearse.ai.call.tokens.{input, output, cached.read, cached.write}`
  - `rehearse.followup.skip.total{reason}`
- **알람**: 미정 (Issue #408)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.service.FollowUpService` | follow-up endpoint 진입점 | called-by → calls |
| `com.rehearse.api.domain.interview.service.FollowUpTransactionHandler` | 트랜잭션 / context 로드 / owner 검증 | calls — guards |
| `com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator` | resume 트랙 라이프사이클 | calls |
| `com.rehearse.api.domain.resume.service.ClockWatcher` | elapsed / remaining 계산 (Clock bean) | calls |
| `com.rehearse.api.domain.interview.service.TurnAnalysisPipeline` | text-only intent + answer 분석 | calls |
| `com.rehearse.api.domain.interview.service.IntentClassifier` | intent 분류 (yml 신뢰도 0.7) | calls |
| `com.rehearse.api.domain.interview.service.AnswerAnalyzer` | answer 분석 + L1 guard | calls |
| `com.rehearse.api.domain.interview.service.IntentDispatcher` | non-ANSWER intent 핸들러 분기 | calls |
| `com.rehearse.api.domain.resume.service.ResumeModeTransitionPolicy` | 모드 전이 결정 (yml `hard-timeout-min:10`) | calls |
| `com.rehearse.api.domain.resume.service.PlaygroundModeHandler` | Playground 처리 | calls |
| `com.rehearse.api.domain.resume.service.InterrogationModeHandler` | Interrogation 처리 | calls |
| `com.rehearse.api.domain.resume.entity.ChainStateTracker` | chain / level 추적 (LEVEL_STAY_MAX_TURNS=2, level cap 4 hardcoded) | calls |
| `com.rehearse.api.domain.resume.service.ResumeQuestionPersister` | question INSERT | calls — persister |
| `com.rehearse.api.domain.resume.service.ResumeTurnEventPublisher` | 도메인 이벤트 발행 | event-publisher |
| `com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent` | resume 트랙 페이로드 (resumeMode / chainLevel / skeleton) | event payload |
| `com.rehearse.api.domain.interview.entity.Interview` | owner / duration / interviewTypes | called-by guard |
| `com.rehearse.api.domain.interview.entity.InterviewRuntimeState` | 세션 런타임 (resume 4종 entity 보유) | mutates |
| `com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache` | Caffeine compute / get / update | calls |
| `com.rehearse.api.domain.resume.service.ResumeSkeletonRuntimeCache` | runtime state.resumeSkeletonCache 래퍼 | calls |
| `com.rehearse.api.domain.resume.service.InterviewPlanRuntimeCache` | runtime state.interviewPlanCache 래퍼 | calls |
| `com.rehearse.api.infra.ai.ResilientAiClient` | LLM 이중화 | calls |
| `com.rehearse.api.infra.ai.AiResponseParser` | parse + schema-hint | calls |
| `com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder` | Playground responder prompt | calls |
| `com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder` | Interrogation prompt | calls |
| `org.springframework.context.ApplicationEventPublisher` | 이벤트 발행 | event-publisher |

---

## 정책 출처

- 모드 전이 임계: `application-*.yml` `hard-timeout-min: 10`
- 4 boolean 임계: `PlaygroundModeHandler` LLM 출력 (a_covered / b_length_ok / c_signal / d_turn_limit) ≥ 2 또는 강제 플래그
- chain 정책: `ChainStateTracker.LEVEL_STAY_MAX_TURNS=2`, level cap = 4 (hardcoded)
- 권한: `Interview.validateOwner(userId)` (`FollowUpTransactionHandler.loadFollowUpContext`)
- intent 신뢰도: yml `rehearse.intent-classifier.fallback-on-low-confidence: 0.7`
- off-topic escalation: yml `rehearse.intent-classifier.off-topic-consecutive-limit: 3`
- LLM 공통 설정: yml `rehearse.resume-track.*`
- Runtime cache: `RuntimeCacheConfig` (TTL 8h, maxSize 10k)
- ❓ 잔존 결정 항목: Issue #408
  - A1 audio 정책 (현 무시 vs 가드 일관성)
  - A4 권한 검증 위치 (`ResumeTrackInitiator` path)
  - B1 dead config (`playground-max-turns`, `chain-max-depth`)
  - B3 L1 False Negative Guard 중복 (entity vs service)
  - C1 `IllegalStateException` 5xx 노출 (BusinessException 래핑 검토)
  - C2 runtime cache evict 명시 호출 (인터뷰 종료 시점)
