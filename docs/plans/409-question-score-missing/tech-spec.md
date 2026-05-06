# Tech Spec — question_score / question_score_dimension 데이터 미적재

> **작성자**: Staff Engineer 페르소나 (backend agent 위임 대상)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off / 검증
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

`question_score` / `question_score_dimension` 미적재 → rubric coaching UX 무효화. 코드 추적으로 가설 확정 → 확정분 fix → Service Integration 회귀 방지로 신규 턴 적재율 정상화.

## Evidence

### 현재 구조 — 3 publish 경로 / 1 listener / 1 persister

```
backend/src/main/java/com/rehearse/api/
├── domain/interview/service/FollowUpService.java
│   ├── line 113-147 generateAndSaveFollowUp (성공) → publishTurnCompletedEvent ✓
│   ├── line 91-104 handleNonAnswerIntent (intent != ANSWER) → ❌ publish 호출 없음
│   ├── line 106-111 handleAnalyzerSkip → ❌ publish 호출 없음
│   └── line 126-131 step B skip 분기 → ❌ publish 호출 없음
├── domain/resume/service/ResumeInterviewOrchestrator.java:87
│   └── turnEventPublisher.publish(..., handlerResult.questionId())  // questionId null 발생 분기 = handler/dispatcher 결함 (근본 fix 대상)
├── domain/resume/service/ResumeTurnEventPublisher.java
│   └── resolveQuestionSet → questionSetRepository.findByInterviewIdAndCategory(...).orElse(null) (보조 방어)
├── domain/feedback/rubric/service/RubricScoringEventListener.java
│   └── @Async @TransactionalEventListener(AFTER_COMMIT) — 5 silent return + generic catch
└── domain/feedback/score/service/QuestionScorePersister.java:27-30
    └── (questionId, rubricId) idempotent skip (uq_question_score 중복 방어)
```

### 가설 확정도

| # | 가설 | 코드 추적 결과 | 상태 |
|---|------|--------------|------|
| 1 | `FollowUpService` 모든 종료 분기 publish 누락 | `publishTurnCompletedEvent` 가 `generateAndSaveFollowUp` 성공 경로에서만 호출 (line 143). `handleNonAnswerIntent` (line 91-104, intent != ANSWER) / `handleAnalyzerSkip` (line 106-111) / step_b_skip 분기 (line 126-131) 모두 미호출 | **확정 (fix — 3 분기 모두 publish 추가)** |
| 2 | Resume 트랙 `handlerResult.questionId()` null → listener questionId null skip | `ResumeInterviewOrchestrator` 가 dispatch 결과를 그대로 publisher 에 전달. handler / dispatcher 의 일부 분기가 questionId 미설정 — 근본 결함 | **확정 (fix — handler/dispatcher 가 questionId 항상 채우도록 수정. publisher null guard 는 보조)** |
| 3 | Resume 트랙 questionSet null → questionSetId null skip | `ResumeTurnEventPublisher.resolveQuestionSet` — questionId null 시 null 반환. Q2 근본 fix (questionId 항상 채움) 후 자연 해소. listener questionSetId null 분기는 결함 게이트로 보존 | **확정 (fix — Q2 의존)** |
| 4 | `(questionId, rubricId)` idempotent skip 으로 동일 question 후속 턴 미적재 | uq_question_score 는 (question_id, rubric_id) 조합. 한 question 은 1번 채점 = 정상. 추가 fix 불필요 | **무효 (정상 동작)** |
| 5 | `score.isEmpty` (CLARIFY/GIVE_UP) silent skip | rubric.selectDimensions 결과 빈 리스트 반환 = 정상 skip. fix 불필요 (Q1 결정으로 publish 는 발생, listener 단 score.isEmpty 정상 skip 으로 일관) | **무효 (정상 동작)** |

### 컨벤션 / 인접 단서

- `backend/.claude/rules/conventions.md` — `@Transactional(readOnly=true)` 기본, app service 트랜잭션, Lombok 제한, 로깅 한국어 + key=value placeholder
- `backend/.claude/rules/testing.md` — Service Integration: 외부 AI 만 Mock, TRUNCATE in @BeforeEach, Testcontainers
- `docs/plans/404-interview-domain-findings/` (인접) — interview 보안 / 안정성. `@Version` 낙관락은 별도 plan
- `AiCallMetrics` 확장 패턴 존재 (`incrementFollowUpSkip(reason)`). 메트릭 후속 작업은 본 plan 비스코프

### 사용자 결정

- 메트릭 카운터 도입 = 본 plan 비스코프 (사용자 결정)
- 가설 코드 추적으로 확정 → fix. 추적 결과 무효 분기 (#4, #5) 수정 X
- **Q1 (FollowUp publish 범위)**: 모든 종료 분기 (intent != ANSWER + analyzer_skip + step_b_skip) publish 추가. 적재 일관성 우선
- **Q2 (Resume questionId null fix 방향)**: handler / dispatcher 근본 수정 (questionId 항상 채움). publisher null guard 는 보조 방어막

## Trade-offs

### Option A (채택): 코드 추적 가설 확정분만 fix + 회귀 테스트
- 장점: 변경 범위 최소. 회귀 표면 작음. 확정된 결함만 수정
- 단점: 운영 메트릭 부재 = 향후 신규 silent skip 분기 추가 시 가시성 0
- 채택 사유: 사용자 결정. 메트릭은 별도 작업

### Option B (폐기): 메트릭 카운터 + 가설 일괄 fix 동시
- 장점: 단일 PR 로 진단 + fix + 회귀 완성
- 단점: scope ↑, 회귀 표면 ↑, 메트릭 namespace 결정 필요
- 폐기 사유: 사용자 메트릭 비스코프 결정

### Option C (폐기): 진단 phase 우선 (메트릭만 도입 후 운영 hit 식별)
- 장점: 미확정 가설까지 데이터로 검증 후 fix
- 단점: 코드 추적으로 이미 3개 가설 확정. 굳이 운영 wait 불필요
- 폐기 사유: 가설 확정 도구로 코드 추적이 충분

## Architecture

### Pre — 결함 시퀀스

```
일반 트랙 (FollowUpService):
  generateAndSaveFollowUp 성공  ─→ publishTurnCompletedEvent ─→ Listener ─→ 적재 ✓
  handleNonAnswerIntent (CLARIFY/GIVE_UP) ─→ ❌ publish 없음 ─→ score 영영 미적재
  handleAnalyzerSkip             ─→ ❌ publish 없음 ─→ score 영영 미적재
  step_b_skip 분기               ─→ ❌ publish 없음 ─→ score 영영 미적재

Resume 트랙 (ResumeInterviewOrchestrator):
  dispatchByMode → handler.handle(...) → handlerResult.questionId() = null  // 근본 결함
       ↓
  ResumeTurnEventPublisher.publish(questionId=null)
       ↓
  resolveQuestionSet(null) → null
       ↓
  TurnCompletedEvent(questionId=null, questionSetId=null) 발행
       ↓
  Listener resolveQuestion null → silent return ─→ score 미적재
```

### Post — 수정 시퀀스

```
일반 트랙 (Q1 결정):
  generateAndSaveFollowUp 성공   ─→ publishTurnCompletedEvent ─→ Listener ─→ score 적재 ✓
  handleNonAnswerIntent          ─→ publishTurnCompletedEvent ─→ Listener ─→ score.isEmpty 정상 skip / 적재 (intent에 따라)
  handleAnalyzerSkip             ─→ publishTurnCompletedEvent ─→ Listener ─→ 동일
  step_b_skip 분기               ─→ publishTurnCompletedEvent ─→ Listener ─→ 동일
  (publish 인자 — questionId: 새 follow-up question 미생성 분기는 turn 의 base questionId 사용 / turnIndex: previousExchanges.size())

Resume 트랙 (Q2 결정 — 근본 fix):
  dispatchByMode → handler.handle(...) → handlerResult.questionId() != null  // 근본 수정
       ↓
  ResumeTurnEventPublisher.publish(questionId=정상)
       ↓ (publisher 보조 방어: 그래도 null 발견 시 log.warn [결함 skip] + 발행 skip)
  resolveQuestionSet 정상 매핑
       ↓
  TurnCompletedEvent 정상 발행 ─→ Listener ─→ 적재 ✓
```

### 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `FollowUpService.java` | 모든 종료 분기 (`handleNonAnswerIntent` / `handleAnalyzerSkip` / step_b_skip) 에서 `publishTurnCompletedEvent` 호출 추가. follow-up question 미생성 분기는 turn 의 base questionId (`request.getQuestionSetId()` / 진행 중 question) + `turnIndex = request.getPreviousExchanges().size()` 인자 사용. publish 시그니처는 기존 유지 |
| Resume `dispatch` / handler 계층 (`ResumeInterviewOrchestrator` + 하위 mode handler 들) | **근본 fix** — `handlerResult.questionId()` 가 모든 정상 분기에서 null 이 아니도록 수정. dispatch 진입 후 questionId 미설정인 분기 식별 → handler 가 question 저장 후 ID 반환하도록 보강. 구체 수정 지점은 backend agent 가 dispatcher / handler 코드 추적 후 implement 단계에서 확정 |
| `ResumeTurnEventPublisher.java` | 보조 방어 — `publish` 진입 시 questionId null 발견 = 결함 (Q2 근본 fix 후 발생 안 해야 함). `[결함 skip]` log.warn + 발행 skip. silent X |
| `ResumeInterviewOrchestrator.java` | dispatch 결과 검증 추가 (questionId nullability 보강 — 근본 fix 의 호출 측 책임) |
| `RubricScoringEventListener.java` | `score.isEmpty` (정상 skip) 와 결함성 skip 구분. log 레벨 / 메시지 정리 (silent → 명시) |
| `RubricScoringEventListenerIntegrationTest` | 신규 — Service Integration. `ServiceIntegrationSupport` 상속 + `TestFixtures` 사용. 외부 AI (`ResilientAiClient`) Mock |
| `FollowUpServiceTest` | 신규 / 보강 — analyzer_skip / step_b_skip 분기 publish 검증 |

## Data Model

**변경 없음.** 기존 `V36__create_question_score.sql` + `uq_question_score(question_id, rubric_id)` 활용.

## API Contract

### 외부 API
**없음.** 사용자 노출 endpoint 변경 X.

### 내부 이벤트 Contract (`TurnCompletedEvent`)
- 페이로드 스키마 변경 **없음**. `ofStandard` / `ofResumeTrack` 팩토리 시그니처 유지.
- `questionId` nullable 정책 유지. listener 의 `questionId == null` 분기는 Resume 트랙의 결함성 skip 게이트로 보존 (정상 skip 과 구분된 로그 처리).
- 발행 측 책임 (Q2 결정 반영): handler / dispatcher 가 questionId 항상 채움. publisher 단 null 발견 = 결함 → 발행 skip + `[결함 skip]` warn 로그 (보조 방어막). 정상 동작 시 listener questionId null 분기는 발생 안 함.

### 로깅 명세 (conventions.md 매핑)
한국어 + key=value placeholder 형식. 정상 / 결함 skip 메시지 prefix 표준화.

```
log.warn("[결함 skip] Resume publish skip — questionId null. interviewId={}, turnIndex={}", ...)
log.debug("[정상 skip] score.isEmpty — intent CLARIFY 등. questionId={}", ...)
log.warn("[결함 skip] Question 미존재. questionId={}", ...)
log.warn("[결함 skip] Listener 예외. interviewId={}, cause={}", ...)
```

운영 모니터링은 `[결함 skip]` prefix 만 추적.

## Verification (완료 판정)

### Service Integration (Testcontainers + TRUNCATE in @BeforeEach)

- [ ] `RubricScoringEventListenerIntegrationTest`
  - [ ] `정상_1턴_TECH_intent_ANSWER → question_score 1행 + dimension N행 적재`
  - [ ] `RESUME_BASED 1턴 정상 → 동일 적재`
  - [ ] `intent CLARIFY_REQUEST → score.isEmpty 정상 skip + 적재 X (회귀 방어)`
  - [ ] `intent != ANSWER 분기 (handleNonAnswerIntent) → publish 발생 + listener score.isEmpty 정상 skip 또는 적재`
  - [ ] `analyzer_skip 분기 → publish 발생 + 적재`
  - [ ] `step_b_skip 분기 → publish 발생 + 적재`
  - [ ] `RESUME_BASED 정상 dispatch → questionId 채워짐 + 적재 (근본 fix 회귀 방어)`

- [ ] `FollowUpServiceTest` 보강
  - [ ] `handleNonAnswerIntent 분기 → TurnCompletedEvent 1회 발행`
  - [ ] `analyzer_skip 분기 → TurnCompletedEvent 1회 발행`
  - [ ] `step_b_skip 분기 → TurnCompletedEvent 1회 발행`
  - [ ] (외부 AI Mock — testing.md 기존 패턴)

- [ ] Resume handler / dispatcher 단위 테스트 보강
  - [ ] `dispatch 결과 questionId null 분기 — 근본 fix 후 0건`
  - [ ] mode별 handler 가 questionId 채워 반환하는지 단위 검증

### Domain Unit / Repository

- [ ] 신규 도메인 로직 없음. Repository 직접 쿼리 변경 없음 → 추가 테스트 불필요

### Smoke / ArchUnit

- [ ] 기존 SmokeTest 통과
- [ ] ArchUnit 영향 없음 (계층 / 패키지 변경 없음)

### 빌드 / 회귀

- [ ] `./gradlew build` 통과
- [ ] 회귀 영역 (기존 패키지 테스트):
  - `interview/service/*Test` (FollowUp / Orchestrator)
  - `feedback/rubric/service/*Test` (Listener / Scorer)
  - `feedback/score/service/*Test` (Persister)
  - `resume/service/*Test` (TurnEventPublisher / Orchestrator)

### 관찰 가능 동작 (dev 검증)

분기 자연 발생 보장 X → **재현 통제 = Service Integration 자동 검증으로 충족**. dev 환경은 보조 검증.

- [ ] dev 배포 후 일반 인터뷰 1턴 → `question_score` row 적재 (DB 직접 쿼리)
- [ ] dev RESUME_BASED 1턴 → 동일
- [ ] docker log `[결함 skip]` prefix 미발생 확인 (정상 케이스)
- [ ] follow-up 미생성 분기 (analyzer_skip / step_b_skip) = Mock 기반 Service Integration 으로 커버. dev 강제 재현은 비스코프

## Pre / Post State

### Pre
- `FollowUpService.handleNonAnswerIntent` (line 91-104) → publish 호출 없음
- `FollowUpService.handleAnalyzerSkip` (line 106-111) → publish 호출 없음
- `FollowUpService` step_b_skip 분기 (line 126-131) → publish 호출 없음
- Resume dispatch / handler 일부 분기 → `handlerResult.questionId()` null
- `ResumeTurnEventPublisher.publish` → questionId null 도 그대로 발행. listener silent return
- `RubricScoringEventListener` 정상 skip / 결함성 skip 로그 구분 없음
- 적재율 추정 0% 부근 (Issue 보고)

### Post
- `FollowUpService` 모든 turn 종료 경로 (intent != ANSWER 포함) `publishTurnCompletedEvent` 발행
- Resume handler / dispatcher 가 questionId 항상 채워 반환 (근본 fix)
- `ResumeTurnEventPublisher` questionId null 발견 시 `[결함 skip]` log.warn + 발행 skip (보조 방어)
- `RubricScoringEventListener` `[정상 skip]` / `[결함 skip]` prefix 로 로그 구분
- Service Integration 4+ 시나리오 회귀 보호
- 적재율 정상화 (dev 1턴 검증 + Service Integration 자동 검증)

## 위험 / 마이그레이션 / 롤백

- **위험**:
  - analyzer_skip / step_b_skip 경로 publish 추가 = listener 호출 빈도 ↑ → AI 호출 (rubric scorer) 빈도 ↑. `score.selectDimensions` 가 intent / mode 따라 빈 리스트 반환 시 `score.isEmpty` early return → AI 호출 자체는 발생 안 함 (`RubricScorer.java:49-53`). 비용 영향 미미
  - listener 진입 빈도 baseline 추정: 현재 정상 분기 1회 / 턴 → fix 후 모든 turn 종료 분기 1회 / 턴. DB read 3건 (Question / QuestionSet / Interview) 추가. 무시 가능 수준
  - `analyzer_skip` 의 intent = `ANSWER` 외 가능. listener 가 적절히 score.isEmpty 처리 — 회귀 표면 작음
  - publisher 단 신규 silent skip 분기 추가 (Q2 옵션에 따라) → 메트릭 부재 시 검증은 log grep + DB row count 만 가능. 본 plan Trade-off 단점 지속
- **NF — 동시성**: idempotent uq (`question_id, rubric_id`) 로 중복 적재 방어. retry / 다중 publish 허용. 추가 락 불필요
- **NF — 부하**: 위 baseline 참조. listener 진입 빈도 < 2배 증가 예상
- **마이그레이션**: DB 변경 없음. 즉시 배포 가능
- **백필 정책**: 과거 미적재 row backfill 안 함 (product-spec 비스코프). 신규 턴부터 적재. 운영 SQL / 사용자 안내는 별도 Issue
- **롤백**: 코드 revert. DB 변경 없음 = 즉시 복구

## 분기 결정

- [x] **단일 영역** → `implement.md` 1개 (BE only)
- [ ] BE+FE 동시 — 해당 없음
- [ ] BE 선행 강제 — 해당 없음

## 후속 (본 plan 비스코프)

- silent skip / return 분기 메트릭 카운터 (`AiCallMetrics` 확장 또는 신규 namespace) — 별도 Issue / 별도 plan
- 적재율 운영 baseline 측정 / 알람 — 메트릭 도입 후속
- 비언어 score (`NonverbalScorePersister`) 적재 결함 — 별도 추적
