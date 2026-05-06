# API: 세션 개시 (opener 생성)

> Endpoint: 별도 endpoint 없음 (`ResumeTrackInitiator.initiate` 내부 마지막 단계)
> Action: resume plan 첫 chain 의 opener 질문 생성 → `RESUME_OPENER` question INSERT, runtime state 초기화
> 관련 테이블: `question` (write, INSERT) / `question_set` (write, find or create RESUME_BASED) / `interview_plan` (read) / `resume_skeleton` (read)
> 관련 외부 의존: OpenAI GPT-4o-mini → Claude Haiku (Playground opener 생성)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| arg | `interviewId` | Long | required | 대상 인터뷰 |
| arg | `userId` | Long | required | 인터뷰 소유자 |

> plan / skeleton 은 직전 단계에서 부착 보장.

---

## 출력 (정상)

| 필드 | 타입 | 의미 |
|------|------|------|
| `questionId` | Long | INSERT 된 opener question PK |
| `questionText` | string | opener 질문 텍스트 |
| `mode` | enum | 항상 `PLAYGROUND` (초기) |

부수 효과:
- `question` INSERT (questionType = `RESUME_OPENER`, V41 chk 만족).
- `question_set` `category=RESUME_BASED` row find or create (V44 인터뷰당 1행 UNIQUE).
- `InterviewRuntimeStateCache.getOrInit(interviewId, ...)` Caffeine 진입 (TTL 8h, maxSize 10k) — `ResumeMode=PLAYGROUND`, `ChainStateTracker` 초기화.

## 출력 (실패)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 409 | `RESUME_PLAN_NOT_READY` | plan 미부착 상태 호출 |
| 502 | `AI_PARSE_FAILED` / `AI_CLIENT_ERROR` / `AI_EMPTY_RESPONSE` / `AI_RESPONSE_INVALID` | LLM 결함 (응답 question text blank → `AI_RESPONSE_INVALID`) |
| 503 | `AI_SERVICE_UNAVAILABLE` | 이중 장애 |
| 504 | `AI_TIMEOUT` | 타임아웃 |
| 429 | `RATE_LIMITED` | Resilience4j |

---

## 흐름

### 1. 사전 검증
- `ResumeInterviewOrchestrator.startSession(interviewId, userId)` 진입 (`@Transactional(propagation = NOT_SUPPORTED)`).
- plan 부착 여부 확인 — 미부착 시 `RESUME_PLAN_NOT_READY` (409).
- 초기 mode = `PLAYGROUND` (`ResumeMode` enum 기본값).

### 2. 기존 RESUME_OPENER 재사용 분기
- `QuestionSetRepository.findByInterviewIdAndCategory(interviewId, RESUME_BASED)` 조회.
- 기존 RESUME_OPENER question 발견 시 **재사용** (재발행 X). 응답에 기존 questionId 반환.
- 미발견 시 다음 단계.

### 3. opener 생성 (LLM)
- `PlaygroundModeHandler.handleOpener(plan, skeleton, runtimeState)` 호출.
- `ResumePlaygroundPromptBuilder.buildOpener` — yml `rehearse.resume-track.{model:gpt-4o-mini, temperature:0.7, max-tokens:800}`.
- callType = `RESUME_PLAYGROUND_OPENER`.
- `AbstractResumeJsonPromptBuilder.executeJson` 경유 → `ResilientAiClient.chat` + `AiResponseParser.parseOrRetry`.
- 응답 question text blank 검증 — blank 시 `AI_RESPONSE_INVALID` (502).
- LLM 응답이 `ResumeFallbackQuestions.OPENER` 와 정확히 같으면 WARN 로그 (LLM-side 폴백 감지 — Issue #408 A3).

### 4. 저장
- `ResumeQuestionPersister.persist(interviewId, opener, type=RESUME_OPENER)` — `@Transactional`.
- `QuestionSet.find or create RESUME_BASED` — 미존재 시 `QuestionSet.builder` + `countByInterviewId` 으로 orderIndex 산출.
- `Question.resume(qs, RESUME_OPENER, text, orderIndex)` factory + `QuestionRepository.save`.
- ⚠ `QuestionSet.questions` collection 미갱신 (단방향). 같은 트랜잭션 내 `qs.getQuestions()` 호출 시 빈 결과 — Issue #408.

### 5. runtime state 초기화
- `InterviewRuntimeStateCache.getOrInit(interviewId, () -> InterviewRuntimeState.seed(...))` — Caffeine `computeIfAbsent` atomic.
- seed: `resumeMode=PLAYGROUND`, `chainStateTracker = new ChainStateTracker()`, `resumeSkeletonCache=skeleton`, `interviewPlanCache=plan`, `playgroundTurns=0`, `resumeOrderCounter=...`.

### 6. 응답
- questionId + 텍스트 + mode 반환. 사용자가 답변하면 `process-user-turn` 으로 진입.

---

## 외부 호출 상세

- Provider chain: `ResilientAiClient` 표준 (OpenAI primary → Claude fallback)
- Sampling: yml `rehearse.resume-track.*` (temperature 0.7, maxTokens 800)
- Retry: OpenAI 2 + Claude 3 = worst 5회 외부 호출
- Parse 실패: schema-hint 1회 → `AI_PARSE_FAILED`
- `RESUME_OPENER` schemaExample 미등록 → schema-hint 시 텍스트 힌트만 (Issue #408 C3)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| plan 미부착 인터뷰 진입 | 409 `RESUME_PLAN_NOT_READY` |
| 기존 RESUME_OPENER question 존재 | 재사용 (재발행 X) — 응답에 기존 questionId 반환 |
| LLM 응답 question text blank | 502 `AI_RESPONSE_INVALID` |
| LLM 응답이 `ResumeFallbackQuestions.OPENER` 와 동일 | WARN 로그 (저장은 진행) — Issue #408 A3 |
| LLM schema 위반 2회 | 502 `AI_PARSE_FAILED` |
| OpenAI + Claude 모두 5xx | 503 `AI_SERVICE_UNAVAILABLE` |
| 동시 startSession 호출 | `Caffeine.computeIfAbsent` atomic 으로 runtime state init 직렬화. question 측은 `find or create` 후 INSERT 시 V44 UNIQUE 충돌 가능성 |

---

## 상태 전이

```
(없음) → PLAYGROUND  (startSession 완료 시 초기 mode 확정)
```

이후 전이 = `process-user-turn.md` 참조.

---

## 관찰성

- **로그**: `[ResumeOrchestrator]` / `[PlaygroundHandler]` / `[ResumeQuestionPersister]`
  - key fields: `interviewId`, `userId`, `mode=PLAYGROUND`, `chainId`, `questionId`, `provider`, `model`, `latencyMs`, `turnCount=0`
- **메트릭** (`AiCallMetrics`):
  - `rehearse.ai.call.duration{call.type=RESUME_PLAYGROUND_OPENER, model, provider, cache.hit, fallback, outcome}`
  - `rehearse.ai.parse.fail.total{stage, call.type=RESUME_PLAYGROUND_OPENER}`
- **알람**: 미정 (Issue #408)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator` | 세션 라이프사이클 진입점 | calls |
| `com.rehearse.api.domain.resume.service.PlaygroundModeHandler` | Playground opener / turn 핸들 | calls |
| `com.rehearse.api.domain.resume.service.ResumeQuestionPersister` | question + question_set INSERT | calls — persister |
| `com.rehearse.api.domain.resume.service.ResumeFallbackQuestions` | LLM-side 폴백 감지 (WARN 로그) | matched |
| `com.rehearse.api.domain.interview.entity.InterviewRuntimeState` | seed / mode / chainStateTracker | mutates |
| `com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache` | Caffeine getOrInit | calls |
| `com.rehearse.api.domain.question.entity.Question / QuestionType.RESUME_OPENER` | factory | persister write |
| `com.rehearse.api.domain.question.repository.QuestionRepository` | save | persister write |
| `com.rehearse.api.domain.questionset.entity.QuestionSet / QuestionSetCategory.RESUME_BASED` | find or create | persister write |
| `com.rehearse.api.domain.questionset.repository.QuestionSetRepository` | findByInterviewIdAndCategory / countByInterviewId / save | reads + writes |
| `com.rehearse.api.infra.ai.ResilientAiClient` | LLM 이중화 | calls |
| `com.rehearse.api.infra.ai.AiResponseParser` | parse + schema-hint | calls |
| `com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder` | opener prompt | calls |
| `com.rehearse.api.domain.question.service.ResumeTrackInitiator` | 직전 단계 호출자 | called-by |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/resume/schema.md` (`question` `RESUME_OPENER` 사용, `question_set` `RESUME_BASED` 인터뷰당 1행 V44 UNIQUE)
- 초기 mode: `ResumeMode.PLAYGROUND` (enum 기본)
- LLM 설정: yml `rehearse.resume-track.*`
- Runtime cache: `RuntimeCacheConfig` (TTL 8h, maxSize 10k)
- ❓ 잔존 결정 항목: Issue #408
  - A3 `ResumeFallbackQuestions` 동기화 정책
  - A4 `ResumeTrackInitiator` 권한 검증 위치
  - C3 `PlaygroundOpenerResult` SchemaExampleRegistry 등록
