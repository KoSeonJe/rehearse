# API: 후속 질문 생성 (꼬리질문)

> Endpoint: `POST /api/v1/interviews/{id}/follow-up`
> Action: 사용자가 메인 질문에 답변 (오디오 + 옵션 텍스트) 후, AI 가 답변을 분석해 다음 흐름 (꼬리질문 / 명확화 / 종료 / 무관 응답) 을 결정한다.
> 관련 테이블: `interview` (read) / `question_set` (read) / `question` (write — followup 추가)
> 관련 외부 의존: OpenAI Whisper (STT) / OpenAI GPT-4o-mini (Step A AnswerAnalyzer + Step B FollowUpQuestionWriter + Intent classifier) + Claude fallback (`ResilientAiClient`)

비동기 진입점: Controller 가 `CompletableFuture.supplyAsync(..., vtExecutor)` 로 가상 스레드 풀에서 실행.

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| header | `Content-Type` | multipart/form-data | required | 오디오 + 메타 동시 업로드 |
| path | `id` | Long | required | 면접 PK |
| part | `request` | JSON `FollowUpRequest` | required | 메타 |
| part.body | `questionSetId` | Long | required | 현재 답변 중인 질문세트 |
| part.body | `questionContent` | string | required, blank 불가 | 사용자가 답한 질문 본문 |
| part.body | `answerText` | string | optional | 클라이언트 STT 결과 (이력서 트랙에서 사용) |
| part.body | `nonVerbalSummary` | string | optional | 비언어 요약 |
| part.body | `previousExchanges` | array | optional | 이전 라운드 [{question, answer, followUpType, selectedPerspective}] |
| part.body | `terminate` | bool | optional, default `false` | RESUME 트랙 전용. FE 시계 잔여 ≤ 0 + 답변 완료 제출 시점에 `true` 동봉. BE 는 답변 분석만 수행 + 신규 question INSERT skip → `followUpExhausted=true` 응답 |
| part | `audio` | multipart | **required** (CS 트랙) | 답변 오디오. 비어있으면 `INTERVIEW_006` |

---

## 출력 (200) — 정상 ANSWER → 꼬리질문 생성

| 필드 | 타입 | 의미 |
|------|------|------|
| `questionId` | Long | 신규 followup `question.id` |
| `question` | string | 꼬리질문 본문 |
| `ttsQuestion` | string | TTS 친화 텍스트 (옵션) |
| `reason` | string | 모델이 생성한 사유 |
| `type` | string | followup type 라벨 |
| `answerText` | string | STT 결과 |
| `modelAnswer` | string | 모범답안 (저장된 question.modelAnswer) |
| `skip` | bool | false |
| `presentToUser` | bool | true (FE 가 렌더링) |
| `followUpExhausted` | bool | 라운드 한계 도달 시 true (FE 가 추가 호출 차단) |
| `selectedPerspective` | string | EXPERIENCE 모드 perspective echo |

## 출력 (200) — Step A / Step B SKIP

```json
{ "skip": true, "skipReason": "analyzer_recommend_skip" | "step_b_skip", "presentToUser": false, ... }
```

## 출력 (200) — Intent 분기 (CLARIFY / GIVE_UP / OFF_TOPIC)

`IntentDispatcher` 가 핸들러 별 응답 (skip=true, presentToUser=true, type=intent).

## 출력 (4xx / 5xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `INTERVIEW_006 ANSWER_TEXT_REQUIRED` | 오디오 파일 비었음 |
| 400 | Bean Validation | `questionSetId` / `questionContent` 누락 |
| 401 | — | JWT 부재 / 만료 |
| 403 | `INTERVIEW_008 FORBIDDEN` | 본인 소유 아님 |
| 404 | `INTERVIEW_001 NOT_FOUND` | 면접 ID 없음 |
| 404 | `QUESTION_SET_NOT_FOUND` | questionSetId 불일치 |
| 409 | `INTERVIEW_003 NOT_IN_PROGRESS` | `status != IN_PROGRESS` |
| 409 | `MAX_FOLLOWUP_EXCEEDED` | 정책 한계 도달 (CS 2 / RESUME 7) |
| 409 | `INTERVIEW_011 FOLLOWUP_DUPLICATE` | 동시 호출로 unique constraint 충돌 |
| 503 | `AI_*` | OpenAI/Claude 모두 실패 (`ResilientAiClient`) |

---

## 흐름

### 1. 비동기 진입
- `CompletableFuture.supplyAsync(..., vtExecutor)` — 가상 스레드 풀 (`AsyncConfig.VT_EXECUTOR`)
- 트랜잭션 경계: `FollowUpService.generateFollowUp` = `@Transactional(propagation = NOT_SUPPORTED)` (메인 메서드는 트랜잭션 미시작 — 외부 AI 호출 중 DB 락 회피)

### 2. 입력 검증
- 오디오 파일 비었음 → `INTERVIEW_006 (400)`
- `loadFollowUpContext(id, userId, questionSetId)` (별도 `@Transactional(readOnly = true)`):
  - `findById` → `validateOwner` (403)
  - `status != IN_PROGRESS` → `INTERVIEW_003 (409)`
  - `questionSetRepository.findById(questionSetId)` → 404
  - `turnPolicyResolver.resolve(interview)` → CS 트랙은 `StandardFollowUpPolicy` (max 2), RESUME 트랙은 `ResumeTrackPolicy` (max 7)
  - `policy.assertCanContinue` → `MAX_FOLLOWUP_EXCEEDED (409)` if 초과
  - `mainReferenceType` 추출 (`MODEL_ANSWER` / `GUIDE`) — Step B 프롬프트 모드 분기

### 3. RuntimeState 초기화
- `runtimeStateStore.getOrInit(id, () -> new InterviewRuntimeState(level.name(), null))`
- Caffeine `computeIfAbsent` 로 동시 호출 1회만 init

### 4. 분기: 트랙 (RESUME vs CS)

#### 4-A. RESUME 트랙 (`isResumeTrack(id, state) == true`)
판정:
1. `state.resumeSkeletonCache != null` → true
2. else: `Interview.interviewTypes.contains(RESUME_BASED)` 확인 → true 면 `ResumeSkeletonPersister.findByInterviewId` 로 캐시 채움 후 true

위임:
- `delegateToResumeOrchestrator`:
  - `resolveResumeSkeleton` (cache → store → `RESUME_PLAN_NOT_READY` if 없음)
  - `resolveInterviewPlan` (caffeine → DB → `ResumeInterviewPlanner.plan(skeleton, durationMinutes)` 신규 생성 후 저장)
  - `ResumeInterviewOrchestrator.processUserTurn(...)` — 이력서 도메인이 FSM 처리 (PLAYGROUND / INTERROGATION). 종료 분기는 FE `terminate=true` 또는 hard timeout backstop

#### 4-B. CS 트랙 (기본)

##### 4-B.1. Audio 분석 (`AudioTurnAnalyzer.analyze`)
- Whisper STT 로 오디오 → 텍스트
- AnswerAnalyzer (Step A): 답변 품질 / claims / missingPerspectives / recommendedNextAction 분석
  - `temperature = 0.2`, `maxTokens = 800`, `responseFormat = JSON_OBJECT`
  - L1 False Negative 가드: `claims 비었음 + answerQuality ≤ 1` 이면 강제 `CLARIFICATION` override
- IntentClassifier 병렬 호출: `temperature = 0.1`, `maxTokens = 200`
  - confidence < 0.7 (`fallback-on-low-confidence`) → forceAnswer
  - 파싱 실패 / 예외 → forceAnswer
- 결과 = `TurnAnalysisResult(intent, answerText, answerAnalysis)`

##### 4-B.2. 분기: intent type

###### intent != ANSWER (CLARIFY_REQUEST / GIVE_UP / OFF_TOPIC)
- `aiCallMetrics.incrementFollowUpSkip("intent_" + intentType)`
- `intentDispatcher.dispatch(intentType, IntentBranchInput(...))`:
  - `CLARIFY_REQUEST` → `ClarifyResponseHandler` → 명확화 응답 생성
  - `GIVE_UP` → `GiveUpResponseHandler` → 격려 + 다음 질문 안내
  - `OFF_TOPIC` → `OffTopicResponseHandler` → `OffTopicEscalationDetector` 가 `consecutive >= 3` (`off-topic-consecutive-limit`) 이면 GIVE_UP 으로 escalate
- 응답: `FollowUpResponse.intentBranch(payload)` (skip=true, presentToUser=true)

###### intent == ANSWER + recommendedNextAction == SKIP
- `aiCallMetrics.incrementFollowUpSkip("analyzer_skip")`
- `FollowUpResponse.aiSkip(answerText, "analyzer_recommend_skip")` (skip=true, presentToUser=false)

###### intent == ANSWER + 정상 → Step B 진행
- `FollowUpQuestionWriter.write(stepBReq, analysis, askedPerspectives)` — 꼬리질문 생성
- Step B 가 skip 반환 → `FollowUpResponse.aiSkip(answerText, stepB.skipReason)` + metrics `step_b_skip`
- 빈 question 응답 → `AiErrorCode.PARSE_FAILED` 예외

##### 4-B.3. 저장 (`FollowUpTransactionHandler.saveFollowUpResult`)
- 별도 `@Transactional` (write) 트랜잭션
- `Question.builder().questionType(FOLLOWUP).orderIndex(N)` → `questionSet.addQuestion()` → `saveAndFlush`
- `DataIntegrityViolationException` (V40 unique constraint `uq_followup_order_index`) → `INTERVIEW_011 FOLLOWUP_DUPLICATE (409)`
- `newFollowUpCount` 계산 → `exhausted = newFollowUpCount >= maxFollowUpRounds`

##### 4-B.4. 이벤트 발행
- `TurnCompletedEvent.ofStandard(...)` 발행 (try-catch — 발행 실패가 턴 진행 차단 안 함, WARN 로그)
- `feedback.rubric` 도메인이 listen — 루브릭 점수 계산 트리거

### 5. 응답
- 200 + `FollowUpResponse(...)` (정상 / skip / intent branch 분기별 빌더 사용)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 같은 questionSetId 동시 호출 | V40 unique `(question_set_id, order_index)` 가 차단 → `INTERVIEW_011 (409)` |
| RESUME 트랙 + skeleton 미생성 | `RESUME_PLAN_NOT_READY` (resume 도메인 에러) |
| Caffeine cache evict 후 재호출 | `isResumeTrack` 가 Interview 엔티티로 재판정 + skeleton 재로딩 |
| OpenAI 실패 → Claude fallback | `ResilientAiClient` 가 자동 처리. 둘 다 실패 시 `AI_*` 503 |
| Intent 파싱 / 분류 실패 | `forceAnswer` (ANSWER 안전 환원) — 사용자 path 차단 안 함 |
| OFF_TOPIC 3회 연속 | `OffTopicEscalationDetector.shouldEscalate(consecutive, 3)` → GIVE_UP 핸들러로 escalate |
| Step A SKIP 권고 | Step B 미호출 (비용 절감) |
| Step B 빈 응답 | `PARSE_FAILED` (스키마 위반 가드) |
| 라운드 한계 도달 후 호출 | `MAX_FOLLOWUP_EXCEEDED (409)`. exhausted=true 응답 후 FE 가 추가 호출 막아야 함 |
| `previousExchanges` null | OffTopic detector / Intent classifier 가 빈 리스트로 안전 처리 |

---

## 상태 전이

본 액션은 `interview.status` 자체를 변경하지 않는다 (`IN_PROGRESS` 유지). `question_set.questions` 컬렉션이 늘어남:

```
질문세트 진행 흐름 (QuestionType):
MAIN(orderIndex=0)
  ├── (FOLLOWUP orderIndex=1)
  └── (FOLLOWUP orderIndex=2)   // CS: max 2 라운드 도달 → exhausted=true
```

이력서 트랙은 `ResumeInterviewOrchestrator` FSM 이 `RESUME_OPENER` / `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 흐름 관리. 종료는 `FollowUpRequest.terminate=true` (FE 시계 만료 시 동봉) 또는 hard timeout backstop (`elapsed ≥ duration + 10분`) 으로 분기.

---

## 관찰성

- **로그**:
  - `FollowUpService` — `[FollowUp] intent != ANSWER 분기: interviewId={}, questionSetId={}, intent={}, confidence={}` (INFO)
  - `Analyzer SKIP 권고 → Step B 미호출. interviewId={}, questionSetId={}` (INFO)
  - `REALTIME 후속 질문 생성 완료(v3): interviewId={}, questionSetId={}, questionId={}, type={}, perspective={}, targetClaim={}, exhausted={}` (INFO)
  - `Step B 가 skip 반환: interviewId={}, questionSetId={}, reason={}` (INFO)
  - `Step B 응답 스키마 위반: skip=false인데 question이 비어있음...` (WARN)
  - `follow-up 중복 삽입 차단 (unique constraint): questionSetId={}, orderIndex={}` (WARN)
  - `TurnCompletedEvent 발행 실패 — 턴 진행 차단하지 않음: interviewId={}, reason={}` (WARN)
- **메트릭** (`AiCallMetrics`):
  - `incrementFollowUpSkip("intent_clarify_request" | "intent_give_up" | "intent_off_topic")`
  - `incrementFollowUpSkip("analyzer_skip")`
  - `incrementFollowUpSkip("step_b_skip")`
  - 외 AI 호출별 latency / token usage (infra/ai/metrics 도메인)
- **알람**: 정책 미정 (❓TODO Issue #404 비스코프 보류)

---

## 동시성 / 트랜잭션 노트

- 메인 메서드 = `NOT_SUPPORTED` (외부 AI 호출 중 DB 락 회피)
- 컨텍스트 로드 = `readOnly = true` (별도 트랜잭션)
- 저장 = `@Transactional` (별도, 짧은 write 트랜잭션)
- Caffeine cache = `computeIfAbsent` atomic — 동시 init 1회 보장
- V40 `uq_followup_order_index` (`question_set_id`, `order_index`) UNIQUE — 중복 INSERT 차단

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.service.AudioTurnAnalyzer` | Whisper STT + Step A 통합 | calls |
| `com.rehearse.api.domain.interview.service.AnswerAnalyzer` | Step A 분석 (Q ↔ A claim / 모호도) | calls — primary GPT-4o-mini |
| `com.rehearse.api.domain.interview.service.IntentClassifier` | intent 분류 | calls — primary GPT-4o-mini |
| `com.rehearse.api.domain.interview.service.FollowUpQuestionWriter` | Step B 꼬리질문 생성 | calls — primary GPT-4o-mini |
| `com.rehearse.api.domain.interview.service.IntentDispatcher` | intent 분기 핸들러 | calls — `ClarifyResponseHandler` / `GiveUpResponseHandler` / `OffTopicResponseHandler` |
| `com.rehearse.api.domain.interview.service.OffTopicEscalationDetector` | OFF_TOPIC 연속 횟수 카운트 | calls |
| `com.rehearse.api.domain.interview.service.FollowUpTransactionHandler` | 컨텍스트 로드 + 저장 (별도 트랜잭션) | calls |
| `com.rehearse.api.domain.interview.service.InterviewTurnPolicyResolver` | 트랙별 정책 해석 (CS / RESUME) | calls |
| `com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache` | Caffeine in-memory state | calls — getOrInit / get / update |
| `com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator` | 이력서 트랙 FSM | calls — RESUME 트랙 위임 |
| `com.rehearse.api.domain.resume.service.{ResumeSkeletonPersister, InterviewPlanPersister, ResumeInterviewPlanner}` | 이력서 트랙 플랜 / skeleton | calls |
| `com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent` | 루브릭 점수 트리거 | event-publisher |
| `com.rehearse.api.infra.ai.AiClient` | LLM 진입점 (`ResilientAiClient`) | calls — primary OpenAI / fallback Claude |
| `com.rehearse.api.infra.ai.WhisperService` | STT (오디오 → 텍스트) | calls — `AudioTurnAnalyzer` 경유 |
| `com.rehearse.api.infra.ai.metrics.AiCallMetrics` | skip / latency 메트릭 | calls |
| `com.rehearse.api.infra.ai.context.InterviewContextBuilder` | 프롬프트 컨텍스트 조립 | calls — Step A / Step B / Intent |
| `com.rehearse.api.global.config.AsyncConfig` (`vtExecutor`) | 가상 스레드 풀 | calls — Controller |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/schema.md` `interview` 섹션 (status invariant) + 도메인 서비스 정책 (트랙별 max-rounds / intent escalation)
- 임계값 (직접 인용):
  - `rehearse.interview.policy.standard.max-follow-up-rounds: 2` (`application.yml:55`) — CS 트랙
  - `ResumeTrackPolicy.HARD_TURN_CAP = 7` — 이력서 트랙
  - `rehearse.intent-classifier.fallback-on-low-confidence: 0.7` (`application.yml:58`)
  - `rehearse.intent-classifier.off-topic-consecutive-limit: 3` (`application.yml:60`)
  - AnswerAnalyzer: `temperature = 0.2`, `maxTokens = 800` (`AnswerAnalyzer.java:31-32`)
  - IntentClassifier: `temperature = 0.1`, `maxTokens = 200` (`IntentClassifier.java:48-50`)
  - L1 FN 가드 임계: `claims.isEmpty() && answerQuality <= 1` (`AnswerAnalyzer.java:93-94`)
  - V40 unique: `(question_set_id, order_index)` — followup 중복 차단
- ❓TODO(사용자 확인) — Issue #404 비스코프 (보류):
  - AI 메트릭 알람 임계 (intent fallback 비율 / Step A skip 비율 / 503 비율 / Step B skip 비율)
