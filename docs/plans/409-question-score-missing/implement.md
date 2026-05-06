# Implement — question_score / question_score_dimension 데이터 미적재 fix

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only). tech-spec `분기 결정 = 단일 영역` 명시.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | Resume mode handler 근본 fix (questionId 항상 채움) + publisher 보조 가드 | `backend` | PR 단일 — commit 1 | - |
| 2 | FollowUpService 3 분기 `publishTurnCompletedEvent` 추가 | `backend` | PR 단일 — commit 2 | - |
| 3 | RubricScoringEventListener 정상/결함 skip 로그 prefix 구분 | `backend` | PR 단일 — commit 3 | - |
| 4 | Service Integration (`RubricScoringEventListenerIntegrationTest`) 신규 | `backend` | PR 단일 — commit 4 | Phase 1, 2, 3 |

> Phase 합 4 ≤ 8, 본문 50줄 미만 → `tasks/` 분리 미적용. 단일 PR 안에 commit 4개 분리 (논리적 작업 단위 = 커밋 1개. `.claude/rules/commit.md`).

---

## Phase 1: Resume mode handler 근본 fix + publisher 보조 가드

- **구현**: `backend` — Resume dispatch 분기에서 `handlerResult.questionId()` 가 항상 non-null 이도록 근본 수정. publisher 단 보조 방어막 추가.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java` — line 91 `Long questionId = null;` 분기 제거. 모든 turn 결과에서 question persist 후 ID 채워 반환. null 반환 분기 식별 + 보강
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` — questionId 채움 분기 / 미채움 분기 코드 추적. 미채움 분기 보강
- `backend/src/main/java/com/rehearse/api/domain/resume/service/WrapUpModeHandler.java` — 회귀 방어 검증 (이미 채움). 변경 없을 가능성
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java` — `TurnHandlerResult.questionId` non-null 보강 (호출 측 검증). dispatch 진입 후 questionId null 발견 시 즉시 fail-fast (BusinessException) 또는 결함 로그
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java` — 보조 가드. `publish` 진입 시 `questionId == null` → `[결함 skip]` log.warn + return. silent X. (Q2 근본 fix 후 발생 안 해야 함 — 방어막)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandlerTest.java` — 단위 테스트 보강 (모든 turn 결과에서 questionId non-null 검증)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/InterrogationModeHandlerTest.java` — 동일
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisherTest.java` — questionId null 발견 시 발행 skip + log.warn 검증 (선택 — 신규 분기 검증 시)

### 핵심 로직

- handler 별 turn 결과 record (`PlaygroundTurnResult` / `InterrogationTurnResult` / `WrapUpTurnResult`) 의 `questionId` 가 모든 정상 분기에서 non-null
- handler 내부에서 `questionPersister.persist(...)` 호출 누락 분기 식별 → persist + ID 채움. 또는 분기 자체 의도적 (질문 저장 X) 면 turn 의 base questionId 활용
- `ResumeInterviewOrchestrator.dispatchByMode` 결과 검증: `result.questionId() == null` = 결함 (Q2 근본 fix 후 발생 안 해야 함). publisher 가 보조 가드
- publisher: `if (questionId == null) { log.warn("[결함 skip] Resume publish skip — questionId null. interviewId={}, ...", ...); return; }`

### 의존

- 선행 phase: 없음 (Phase 2/3 와 독립 — 병렬 가능. 단 단일 PR 이라 직렬 진행)
- 외부: 없음

### Verification Hook

- 명령:
  - `./gradlew test --tests "com.rehearse.api.domain.resume.*"`
  - `./gradlew compileJava`
- 통과 기준: 모든 resume 도메인 테스트 green. 신규 단위 테스트 (handler 별 questionId non-null 검증) 통과
- 관찰 가능 동작: dev 환경 RESUME_BASED 1턴 → `question_score` row 적재 (Phase 4 통합 검증 후)

### 커밋 메시지

```
fix(BE): Resume mode handler 가 questionId 항상 채우도록 근본 수정
```

---

## Phase 2: FollowUpService 3 분기 `publishTurnCompletedEvent` 추가

- **구현**: `backend` — FollowUpService 의 모든 turn 종료 분기에서 `TurnCompletedEvent` 발행하도록 보강. 적재 일관성 확보.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java`
  - line 91-104 `handleNonAnswerIntent` (intent != ANSWER, e.g. CLARIFY/GIVE_UP) → return 직전 `publishTurnCompletedEvent` 호출 추가
  - line 106-111 `handleAnalyzerSkip` → return 직전 호출 추가
  - line 126-131 step_b_skip 분기 → return 직전 호출 추가
- `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpServiceTest.java` — 3 분기 publish 검증 보강. `ApplicationEventPublisher` Mock 으로 호출 횟수 / 인자 검증

### 핵심 로직

- publish 시그니처 기존 유지 (`publishTurnCompletedEvent(interviewId, context, turn, questionId, turnIndex)`)
- follow-up question 미생성 분기 (3 분기 모두) 의 인자:
  - `questionId` = turn 의 base questionId (`request.getQuestionSetId()` 기반 진행 중 question 의 ID. 이미 dispatch 시 사용된 값)
  - `turnIndex` = `request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size()` (handleNonAnswerIntent 의 기존 turnIndex 산출 패턴 동일)
- `handleAnalyzerSkip` 도 동일 turnIndex 산출 적용
- step_b_skip 분기 (line 126-131) 도 동일

### 의존

- 선행 phase: 없음 (Phase 1 과 독립)
- 외부: 없음

### Verification Hook

- 명령: `./gradlew test --tests "FollowUpServiceTest"`
- 통과 기준: 3 분기별 publish 1회 발생 검증 + 기존 정상 분기 회귀 통과
- 관찰 가능 동작: dev 환경에서 follow-up 미생성 턴 발생 시 `question_score` row 적재 (Phase 4 통합 검증)

### 커밋 메시지

```
fix(BE): FollowUpService 모든 종료 분기 TurnCompletedEvent 발행
```

---

## Phase 3: RubricScoringEventListener 정상/결함 skip 로그 prefix 구분

- **구현**: `backend` — 5 silent return + generic catch 의 로그 메시지에 `[정상 skip]` / `[결함 skip]` prefix 부여. 운영 모니터링에서 결함만 추적 가능하게.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java`
  - `score.isEmpty` (CLARIFY 등 정상 skip) → `log.debug("[정상 skip] score.isEmpty — intent={}. questionId={}", ...)`
  - questionId null → `log.warn("[결함 skip] Question 식별 불가 — questionId null. interviewId={}, turnIndex={}", ...)`
  - questionSetId null → `log.warn("[결함 skip] QuestionSet 식별 불가 — questionSetId null. interviewId={}, questionId={}", ...)`
  - Question 미존재 → `log.warn("[결함 skip] Question 미존재. questionId={}", ...)`
  - generic catch → `log.warn("[결함 skip] Listener 예외. interviewId={}, cause={}", ..., e)`
  - 메시지 한국어 + key=value placeholder (conventions.md Logging)

### 핵심 로직

- 메시지 포맷 변경만. 동작 변경 X (회귀 표면 0)
- 정상 = debug (운영 noise X). 결함 = warn (운영 추적 대상)
- 운영 모니터링 = `[결함 skip]` prefix 만 grep / 알람 후속

### 의존

- 선행 phase: 없음 (Phase 1, 2 와 독립)
- 외부: 없음

### Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.domain.feedback.rubric.*"`
- 통과 기준: 기존 listener 테스트 회귀 통과 (메시지 포맷 변경만)

### 커밋 메시지

```
refactor(BE): RubricScoringEventListener skip 로그 정상/결함 prefix 구분
```

---

## Phase 4: Service Integration — `RubricScoringEventListenerIntegrationTest` 신규

- **구현**: `backend` — Phase 1-3 통합 시나리오 회귀 방어. tech-spec Verification 시나리오 100% 커버.

### 변경 파일

- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListenerIntegrationTest.java` — 신규
  - `ServiceIntegrationSupport` 상속 (Testcontainers MySQL + Spring 컨텍스트 + TRUNCATE in `@BeforeEach`)
  - `TestFixtures` 사용 (Interview / Question / QuestionSet / User 등)
  - 외부 AI = `ResilientAiClient` Mock (testing.md "외부 API 만 Mock" 룰)
  - `@DisplayName` 한국어 + `@Nested` 시나리오 그룹

### 시나리오 (tech-spec Verification 그대로)

- [ ] `정상_1턴_TECH_intent_ANSWER → question_score 1행 + dimension N행 적재`
- [ ] `RESUME_BASED 1턴 정상 dispatch → 동일 적재 (Phase 1 근본 fix 회귀 방어)`
- [ ] `intent CLARIFY_REQUEST → score.isEmpty 정상 skip + 적재 X`
- [ ] `intent != ANSWER (handleNonAnswerIntent) → publish 발생 + score.isEmpty 정상 skip 또는 적재`
- [ ] `analyzer_skip 분기 → publish 발생 + 적재`
- [ ] `step_b_skip 분기 → publish 발생 + 적재`
- [ ] `Resume publish 시 questionId null (결함 케이스 강제) → 발행 skip + [결함 skip] log (silent X)`

### 핵심 로직

- 각 시나리오 = 실제 publish 발생 → AFTER_COMMIT listener 비동기 실행 → DB row 적재 검증
- 비동기 listener 검증: `Awaitility` 또는 동기화 wait. 기존 패턴 따름
- AI Mock = `RubricScorer` 가 호출하는 `ResilientAiClient` 만 stub (rubric 응답 fixture 반환)

### 의존

- 선행 phase: Phase 1, 2, 3 모두 (통합 검증)
- 외부: Testcontainers MySQL

### Verification Hook

- 명령:
  - `./gradlew test --tests "RubricScoringEventListenerIntegrationTest"`
  - `./gradlew build` (전체 빌드 + 회귀)
- 통과 기준: 모든 시나리오 green. 회귀 영역 (`interview/service/*Test`, `feedback/rubric/service/*Test`, `feedback/score/service/*Test`, `resume/service/*Test`) 통과
- 관찰 가능 동작: dev 배포 후 일반 / RESUME_BASED 1턴 → `question_score` row 적재 (수동 DB 쿼리)

### 커밋 메시지

```
test(BE): question_score 적재 회귀 방어 Service Integration 추가
```

---

## 통합 Verification

- [ ] `tech-spec.md#verification` 항목 모두 통과 (Service Integration 7 시나리오 + 단위 보강 + Smoke + ArchUnit + 빌드)
- [ ] 회귀 영역 4 패키지 (`interview/service`, `feedback/rubric/service`, `feedback/score/service`, `resume/service`) green
- [ ] dev 배포 후 보조 검증: 일반 / RESUME_BASED 1턴 → `question_score` row 적재 (DB 직접 쿼리) + docker log `[결함 skip]` prefix 미발생

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md `Pre / Post State` 섹션 기준)
