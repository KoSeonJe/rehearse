# Implement (Backend) — 이력서 면접 트랙 단순화 + 분석 모델 재설계

> **작성자**: backend agent
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★
> **출처**: `tech-spec.md` §Architecture / §Data Model / §분기 결정

---

## Phase 0: API Contract 확인

`tech-spec.md` §API Contract (L408-446) 확정 상태.

- [x] Endpoint: `POST /api/v1/interviews/{id}/follow-up` (변경 X, 응답 schema 만 변경)
- [x] Response schema 변경 — `selectedAnswerFeedbackPerspective` 필드 제거
- [x] Request schema 변경 — `FollowUpRequest.FollowUpExchange.selectedAnswerFeedbackPerspective` 필드 제거
- [x] QuestionType enum literal 변경 — `RESUME_PLAYGROUND`/`RESUME_INTERROGATION` 폐기 + `RESUME_MAIN`/`RESUME_FOLLOWUP` 신설
- [x] 강결합 = BE 선행 강제 (FE 단독 머지 시 신규 enum literal 도달 0)

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | P1 Rubric 디커플링 | `backend` | #N | Phase 0 |
| 2 | P2 신규 흐름 + 분석 모델 (15 task → tasks/ 분리) | `backend` | #N+1 | Phase 1 머지 |
| 3 | P3 잔여 정리 | `backend` | #N+2 | Phase 2 머지 |

> Phase 2 = 15 task → `tasks/p2-be-NN-{slug}.md` 분리. Phase 1 / 3 = 단일 본문 inline.

---

## Phase 1: P1 — Rubric 디커플링

- **구현**: `backend` — Rubric 점수 산출에서 `resumeMode` / `currentChainLevel` / `resumeSkeleton` 3개 nullable 파라미터 제거 + `_mapping.yaml` 폐기 + `TurnCompletedEvent` 재명명

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/Rubric.java` — `selectDimensions(ResumeMode)` → `selectDimensions()` 무인자. `perTurnRules` mode key (`on_playground_mode`/`on_interrogation_mode`) 사용처 제거
- `backend/src/main/resources/rubric/*.yaml` — `perTurnRules` mode key 정리 (resume / behavioral / experience-* / concept-*)
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java` — `score(...)` 시그니처 `ResumeMode` / `Integer currentChainLevel` / `ResumeSkeleton` 3개 파라미터 제거
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java` — `event.resumeMode` / `currentChainLevel` / `resumeSkeleton` 참조 제거
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/event/TurnCompletedEvent.java` → `FollowUpQuestionCreatedEvent.java` (파일 재명명) — 7 필드 (`interviewId`, `userId`, `questionId`, `questionSetId`, `userAnswer`, `analysis`, `userLevel`). `turnIndex` + resume 3 필드 폐기. `ofStandard`/`ofResumeTrack` → `of(...)` 단일
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandler.java` — `publishTurnCompletedEvent` → `publishFollowUpQuestionCreatedEvent` 재명명. 시그니처 갱신
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricLoader.java` — `loadMapping()` / `MappingResult` / `MappingRule` / `RubricResolutionContext` / `RubricMappingFamily.resolve(ctx)` 폐기. 신규 private `resolveRubricId(InterviewType, QuestionType)` switch 룰. `resolveFor(question, questionSet, interview)` 외부 시그니처 유지 (내부만 변경). `always_apply.nonverbal-v1` = 상수 `ALWAYS_APPLIED_RUBRIC_IDS`
- `backend/src/main/resources/rubric/_mapping.yaml` — 파일 삭제
- `backend/src/test/.../RubricLoaderTest.java` — 매핑 6 룰 + default 회귀 fixture
- `backend/src/test/.../RubricSnapshotTest.java` — 기존 fixture (BEHAVIORAL / TECH / EXPERIENCE 각 1) diff 0

### 핵심 로직

```
RubricLoader.resolveRubricId(InterviewType type, QuestionType questionType):
  switch (type):
    RESUME_BASED → "resume-v1"
    CS_FUNDAMENTAL → "concept-cs-fundamental-v1"
    LANGUAGE_FRAMEWORK, UI_FRAMEWORK → "concept-lang-framework-v1"
    BEHAVIORAL → "experience-collaboration-v1"
    default → questionType.category() == EXPERIENCE ? "experience-technical-v1" : "fallback-generic-v1"

Rubric.selectDimensions():
  perTurnRules.get("on_intent_answer") ?? usesDimensions.map(DimensionRef::ref)
```

### 의존
- 선행: Phase 0 (contract 합의)
- 외부: 없음

### Verification
- `./gradlew test --tests RubricTest` — `selectDimensions()` 무인자 호출. `resumeMode` 인자 메서드 부재 컴파일 검증
- `./gradlew test --tests RubricLoaderTest` — 매핑 6 룰 모두 기존 결과 동일
- `./gradlew test --tests RubricLoaderResolveTest` — `_mapping.yaml` 파일 부재 + Java switch 결과 일치
- `./gradlew test --tests RubricSnapshotTest` — BEHAVIORAL / TECH / EXPERIENCE fixture 점수 diff 0
- 통과 기준: 모든 테스트 green + grep `_mapping.yaml` / `RubricMappingFamily` / `MappingResult` / `MappingRule` / `RubricResolutionContext` / `selectDimensions(ResumeMode` / `ofResumeTrack` / `ofStandard` / `publishTurnCompletedEvent` 잔존 0

### 커밋 메시지

```
refactor(BE): Rubric 디커플링 - mode/chainLevel/skeleton 파라미터 + _mapping.yaml 폐기
```

---

## Phase 2: P2 — 신규 흐름 + 분석 모델 재설계

- **구현**: `backend` — 이력서 트랙을 표준 트랙 패턴으로 흡수 + AnswerAnalysis 재설계 (dimensionGaps 단일 축) + 모든 FSM / Plan / Compactor / RuntimeState 폐기

### Task 목차 (tasks/ 분리)

| # | 파일 | 핵심 책임 | 의존 |
|---|------|---------|------|
| 01 | [p2-be-01-question-type.md](tasks/p2-be-01-question-type.md) | QuestionType enum 정리 (RESUME_MAIN/FOLLOWUP 추가, PLAYGROUND/INTERROGATION 폐기) | - |
| 02 | [p2-be-02-answer-analysis.md](tasks/p2-be-02-answer-analysis.md) | AnswerAnalysis 5필드 재설계 + TurnAnalysis interface/Result/Pipeline 폐기 | 01 |
| 03 | [p2-be-03-prompt-template.md](tasks/p2-be-03-prompt-template.md) | Prompt template `missing_perspectives` → `dimension_gaps` + `weakest_dimension` | 02 |
| 04 | [p2-be-04-dto-cleanup.md](tasks/p2-be-04-dto-cleanup.md) | FollowUp DTO `selectedAnswerFeedbackPerspective` 필드 제거 | 03 |
| 05 | [p2-be-05-resume-track-initiator.md](tasks/p2-be-05-resume-track-initiator.md) | ResumeTrackInitiator 책임 재작성 (skeleton ingest + LLM 1회 + saveResults) | 01 |
| 06 | [p2-be-06-followup-service.md](tasks/p2-be-06-followup-service.md) | FollowUpService `delegateToResumeOrchestrator` 분기 + Resume 의존 제거 | 02, 05 |
| 07 | [p2-be-07-standard-policy.md](tasks/p2-be-07-standard-policy.md) | StandardFollowUpPolicy RESUME_OPENER skip 분기 추가 | 01, 06 |
| 08 | [p2-be-08-followup-writer-prompt.md](tasks/p2-be-08-followup-writer-prompt.md) | FollowUpQuestionWriter prompt `weakestDimension` + skeleton 동봉 | 02, 03 |
| 09 | [p2-be-09-resume-fsm-discard.md](tasks/p2-be-09-resume-fsm-discard.md) | Resume FSM 25+ 클래스 폐기 (Orchestrator/Handler/Chain/Plan) | 05, 06 |
| 10 | [p2-be-10-context-discard.md](tasks/p2-be-10-context-discard.md) | DialogueCompactor/HistoryLayer/SessionStateLayer + Focus 분기 폐기 | 06, 08 |
| 11 | [p2-be-11-runtime-state-discard.md](tasks/p2-be-11-runtime-state-discard.md) | InterviewRuntimeState 3 클래스 전면 폐기 + consumer 직접 주입 | 09, 10 |
| 12 | [p2-be-12-log-masking.md](tasks/p2-be-12-log-masking.md) | infra/ai/logging payload 마스킹 정책 점검 + 추가 | 05 |
| 13 | [p2-be-13-tests.md](tasks/p2-be-13-tests.md) | Service Integration + E2E + 결정적 회귀 + 로그 마스킹 테스트 | 01-12 |
| 14 | [p2-be-14-dev-sql.md](tasks/p2-be-14-dev-sql.md) | `scripts/dev-cleanup-resume-legacy.sql` (Flyway 분리, 수동 실행) | - |
| 15 | [p2-be-15-ai-metrics.md](tasks/p2-be-15-ai-metrics.md) | AiCallMetrics `resume_question_generator` 추가 + `compaction_summarizer` panel 메모 | 05, 10 |

### 의존 (Phase 단)
- 선행: Phase 1 머지 (Rubric event 시그니처 정리 선행)
- 외부: `ResilientAiClient` (GPT-4o-mini primary + Claude fallback)

### Verification (Phase 단)
- `./gradlew test` 전체 green
- §통합 Verification 참조

### 커밋 메시지 (Phase 단 — 단일 PR 단일 커밋)

```
refactor(BE): 이력서 트랙 표준 패턴 흡수 + AnswerAnalysis dimension 단일 축 재설계
```

---

## Phase 3: P3 — 잔여 정리

- **구현**: `backend` — ResumeSkeleton 슬림화 + interview_plan 테이블 DROP + ArchUnit 가드 + Fallback 클래스 폐기

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeleton.java` — `interrogationPriorityMap` 필드 + `priorityIds(...)` 메서드 제거
- `backend/src/test/.../ResumeSkeletonJsonCompatibilityTest.java` — 기존 `interrogationPriorityMap` 포함 JSON payload 입력 시 `@JsonIgnoreProperties(ignoreUnknown = true)` 무시 + record 생성 성공
- `backend/src/main/resources/db/migration/V48__drop_interview_plan.sql` — `DROP TABLE IF EXISTS interview_plan;` DDL 1개 (DML 금지)
- `backend/src/test/.../ResumeArchitectureTest.java` — ArchUnit 단언 (§Appendix A 클래스 부재 + `applyL1FalseNegativeGuard` 메서드 부재 + `TurnAnalysis` interface 부재)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackQuestions.java` — 파일 삭제
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackBestAnswers.java` — 파일 삭제

### 핵심 로직

```
ResumeSkeleton 재정의:
  record ResumeSkeleton(
    String resumeId, String fileHash, CandidateLevel candidateLevel,
    String targetDomain, List<Project> projects
  )
  // interrogationPriorityMap / priorityIds 제거

V48 DDL:
  DROP TABLE IF EXISTS interview_plan;

ResumeArchitectureTest (ArchUnit):
  noClasses().that().haveSimpleName(in §Appendix A-1).should().exist()
  noMethods().that().haveName("applyL1FalseNegativeGuard").should().exist()
```

### 의존
- 선행: Phase 2 머지 (Resume FSM / Runtime / Context 폐기 선행)
- 외부: ArchUnit 라이브러리 (기존 의존)

### Verification
- `./gradlew test --tests ResumeSkeletonJsonCompatibilityTest` — JSON 호환 회귀
- `./gradlew test --tests ResumeArchitectureTest` — ArchUnit 가드 통과
- `./gradlew flywayMigrate` — V48 적용 (local 검증)
- grep `interrogationPriorityMap` / `priorityIds` / `ResumeFallbackQuestions` / `ResumeFallbackBestAnswers` 잔존 0
- 통과 기준: tech-spec §Appendix A 전체 grep 잔존 0 + ArchUnit green

### 커밋 메시지

```
chore(BE): Resume 트랙 잔여 정리 - skeleton 슬림 + interview_plan DROP + ArchUnit 가드
```

---

## FE 와 통합 시점

- Phase 1 머지 = FE 영향 0 (BE 내부)
- Phase 2 머지 = QuestionType enum literal 변경 시점 = FE 통합 트리거. `implement-fe.md` Phase 1 시작 신호
- Phase 3 머지 = FE 영향 0 (BE 내부)

순서: Phase 1 머지 → Phase 2 BE PR 머지 → FE PR 머지 → Phase 3 머지

## 통합 Verification

tech-spec.md §Verification 항목 모두 통과 강제:

- [ ] `ResumeTrackInitiatorIntegrationTest` — opener N + main M=duration/3+2 일괄 생성. RESUME_OPENER=EXPERIENCE, RESUME_MAIN=TECHNICAL, projects 범위 topic
- [ ] `FollowUpServiceResumeFlowTest` — RESUME_OPENER 응답 후 follow-up 미생성 / RESUME_MAIN 응답 후 RESUME_FOLLOWUP 1개 생성
- [ ] `AnswerAnalysisTest` — 신규 record 컴파일 + `applyL1FalseNegativeGuard` 부재
- [ ] `RubricTest` / `RubricLoaderResolveTest` — Phase 1 회귀
- [ ] `AnswerAnalyzerPromptRenderingTest` — `dimension_gaps` / `weakest_dimension` 토큰 포함, `missing_perspectives` 토큰 부재
- [ ] `ResumeInterviewE2ETest` — `/interviews` 생성 → IN_PROGRESS → opener → main → follow-up → 시간 만료 종료
- [ ] `ResumeRepetitionDeterministicTest` — interview 29 trace 모킹 LLM 시퀀스 동일 topic 3회 이상 출제 시 fail
- [ ] `ResumeRepetitionRegressionTest` (`@Disabled` + `@EnabledIfEnvironmentVariable RUN_LIVE_API`) — Live LLM 회귀
- [ ] `RubricSnapshotTest` — BEHAVIORAL / TECH / EXPERIENCE fixture diff 0
- [ ] `FollowUpServiceTest` / `BehavioralFollowUpServiceTest` — 표준 트랙 회귀
- [ ] `ResumeSkeletonJsonCompatibilityTest` — `interrogationPriorityMap` JSON 호환
- [ ] `ResumeArchitectureTest` — Phase 3 ArchUnit 가드
- [ ] `ResumeTrackInitiatorLoggingTest` — INFO 로그 payload 부재
- [ ] `./gradlew build` 통과
- [ ] grep — tech-spec §Appendix A 전체 잔존 0

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (각 Phase 머지 직전 — 메인 세션 책임)
- [ ] Phase 2 = `code-reviewer-frontend` 와 **병렬** 호출 (BE+FE 동시 작업, 단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff = tech-spec §Pre/Post State 일치
