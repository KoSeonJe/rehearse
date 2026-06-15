# API: 턴 채점 (이벤트 기반)

> Endpoint: 없음 (내부 이벤트 — `TurnCompletedEvent` publish)
> Action: 답변 1턴 완료 시 LLM 루브릭 채점 비동기 실행
> 관련 테이블: `question_score` (write), `question_score_dimension` (write)
> 관련 외부 의존: OpenAI GPT-4o-mini (`ResilientAiClient` 경유, fallback Claude `claude-sonnet-4-20250514`)

---

## 입력 (이벤트)

| 필드 | 타입 | 의미 |
|------|------|------|
| `interviewId` | Long | 인터뷰 |
| `turnIndex` | int | 턴 순번 |
| `userId` | Long | 소유자 |
| `questionId` | Long | 대상 질문 |
| `questionSetId` | Long | 세트 |
| `userAnswer` | string | 답변 텍스트 |
| `answerAnalysis` | object | 직전 분석 (clarity 등) |
| `intent` | enum | FOLLOW_UP / CLARIFY_REQUEST / GIVE_UP / PLAYGROUND / INTERROGATION 등 |
| `level` | enum | InterviewLevel |
| `resumeMode?` | enum | resume 트랙일 때 |
| `currentChainLevel?` | int | resume 깊이 |
| `resumeSkeleton?` | object | resume 컨텍스트 |

---

## 흐름

### 1. Publisher
- 표준: `FollowUpService.publishTurnCompleted` → `TurnCompletedEvent.ofStandard`
- Resume: `ResumeTurnEventPublisher.publish` → `TurnCompletedEvent.ofResumeTrack`

### 2. Listener
- `RubricScoringEventListener` — `@TransactionalEventListener(AFTER_COMMIT)` + `@Async(rubricScoringExecutor)` (Virtual Thread per task)

### 3. 분기: intent 별 dimension 선택
- `RubricCatalog.resolveFor(question, questionSet, interview)` — `RubricFamily.MappingRule` 체인 (resumeTrack → category → feedbackPerspective → defaultRubricId)
- `Rubric.selectDimensions(intent, resumeMode)` — switch:
  - `CLARIFY_REQUEST` / `GIVE_UP` → 빈 dimensions → **persist skip**
  - `PLAYGROUND` / `INTERROGATION` → per_turn_rules YAML 분기
  - 그 외 → 기본 dimension set

### 4. 외부 호출
- Provider: `gpt-4o-mini` (`RubricScorerPromptBuilder` model=`gpt-4o-mini`, temperature=`0.2`)
- Client: `ResilientAiClient` — primary 실패 시 Claude fallback (`claude-sonnet-4-20250514`)
- `OpenAiClient`: connect 5s / read 60s / MAX_RETRY_ATTEMPTS=2 / INITIAL_RETRY_DELAY_MS=1000 (exponential)
- 4xx (≠429) → CLIENT_ERROR (no retry) / 429·5xx → RetryableApiException / 소진 시 TIMEOUT
- 두 provider 모두 실패 → 503 SERVICE_UNAVAILABLE (listener 가 흡수, turn 진행 차단 X — silent #406 D8)

### 5. Parse / 검증
- `RubricScoringAdapter`: SCORE_MIN=1, SCORE_MAX=3 (out-of-range → null)
- `evidence_quote` 누락 시 1회 schema retry, 재실패 시 score 무효화 (NA)
- 2차 파싱 실패 → fallback all NA

### 6. 저장
- `QuestionScorePersister.saveRubric` — UNIQUE(question_id, rubric_id) idempotent upsert
- dimension 별 `question_score_dimension` insert — DB UNIQUE 부재, 어플리케이션 책임 (#406 D3)

### 7. 응답
- 이벤트 — 응답 없음
- 실패 시 `aiCallMetrics.incrementRubricFailure("persist_failed" | …)` — UI 노출 없음 silent (#406 G2 / D8)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| intent=CLARIFY_REQUEST/GIVE_UP | dimensions empty → 본 턴 score 미생성 (정상) |
| OpenAI + Claude 모두 실패 | 503 → listener 흡수, turn 사용자 경험 차단 X. 메트릭 카운트만 (silent fail, #406 G2) |
| 동일 (question, rubric) 재진입 | idempotent upsert (UNIQUE) |
| evidence_quote 누락 | 1회 schema retry → 실패 시 score=null (NA) |

---

## 관찰성

- **로그**: `RubricScoringEventListener` — `interviewId`, `turnIndex`, `intent`, `rubricId`, `provider`, `latencyMs`
- **메트릭**: `aiCallMetrics.incrementRubricFailure(reason)` — reason ∈ {persist_failed, parse_failed, …}

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.interview.service.FollowUpService` | TurnCompletedEvent.ofStandard | event-publisher |
| `com.rehearse.api.domain.resume.service.ResumeTurnEventPublisher` | TurnCompletedEvent.ofResumeTrack | event-publisher |
| `com.rehearse.api.domain.feedback.rubric.service.RubricScoringEventListener` | 비동기 채점 | event-listener |
| `com.rehearse.api.domain.feedback.rubric.service.RubricScorer` | dimension 선택 + LLM call | calls |
| `com.rehearse.api.domain.feedback.rubric.service.RubricLoader` | yaml 로딩 (`_dimensions.yaml`, `_mapping.yaml`, `*-rubric.yaml`) | calls |
| `com.rehearse.api.infra.ai.adapter.RubricScoringAdapter` | LLM 결과 → dimension 매핑 + 검증 | calls |
| `com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder` | model=gpt-4o-mini / temp=0.2 | calls |
| `com.rehearse.api.infra.ai.ResilientAiClient` | primary + fallback | calls |
| `com.rehearse.api.global.config.RubricScoringExecutorConfig` | Virtual Thread executor | infra |

---

## 정책 출처

- 모델 / 온도: `RubricScorerPromptBuilder` (`gpt-4o-mini`, `0.2`) — primary 만 명시. Claude fallback temp 일치 미검증 (#406 D6)
- score range: `RubricScoringAdapter` SCORE_MIN=1, SCORE_MAX=3
- HTTP timeout / retry: `OpenAiClient` connect=5s, read=60s, MAX_RETRY_ATTEMPTS=2, INITIAL_RETRY_DELAY_MS=1000
- intent → dimension 매핑: `Rubric.selectDimensions(intent, resumeMode)` switch
- Claude fallback 모델: `application-{local,dev,prod}.yml` `claude.model=claude-sonnet-4-20250514`
- 사용자 노출 정책 = #406 G2 (silent → 종합 배지 + 사용자 retry 신설)
