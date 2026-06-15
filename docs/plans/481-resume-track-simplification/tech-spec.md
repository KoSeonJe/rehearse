# Tech Spec — 이력서 면접 트랙 단순화 + 분석 모델 재설계

> **작성자**: backend agent
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

이력서 트랙 (Mode FSM + Chain FSM + InterviewPlan + 모드 핸들러 + 다층 컨텍스트 + L1FN 가드) → 표준 트랙 패턴 흡수 + 분석 모델 Rubric dimension 단일 축 통일. 동일 주제 반복 출제 차단 + 디버깅 가시성 확보 + 트랙별 분기 코드 0.

## Evidence

### 현재 구조
- **표준 트랙 follow-up 흐름**: `FollowUpService.generateFollowUp()` line 64-79 — context load → `AudioTurnAnalyzer.analyze` → `FollowUpQuestionWriter.write` → `FollowUpTransactionHandler.saveFollowUpResultAndPublishEvent`. RESUME 트랙은 line 67-69 `delegateToResumeOrchestrator` 분기로 우회
- **이력서 트랙 진입점**: `ResumeInterviewOrchestrator.processUserTurn(...)` (256줄). PLAYGROUND ↔ INTERROGATION 2단계 FSM + ChainStateTracker 4-level FSM + InterviewPlan 사전 계획
- **분석 모델**: `AnswerAnalysis.java` — `turnId / claims / missingPerspectives:List<AnswerFeedbackPerspective> / unstatedAssumptions / answerQuality:int / recommendedNextAction`. `applyL1FalseNegativeGuard()` 호출자 = `AnswerAnalyzer:81` + `AudioTurnAnalyzer:94`
- **Rubric 결합점**:
  - `Rubric.selectDimensions(ResumeMode)` line 23-41 — mode 별 perTurnRules key (`on_playground_mode` / `on_interrogation_mode` / `on_intent_answer`) 분기
  - `RubricScorer.score(...resumeMode, currentChainLevel, resumeSkeleton)` line 33-42 — 3개 nullable 파라미터
  - `TurnCompletedEvent.ofResumeTrack(...)` line 37-51 — `resumeMode / currentChainLevel / resumeSkeleton` 3 nullable 필드
- **QuestionType**: `RESUME_OPENER(EXPERIENCE) / RESUME_PLAYGROUND(EXPERIENCE) / RESUME_INTERROGATION(TECHNICAL)`. `isMain()` / `isFollowUp()` 은 TECH/BEHAVIORAL 만 true → 표준 follow-up 코드 경로 재사용 불가
- **InterviewRuntimeState**: 13 필드 보유. 본 plan 후 모든 필드 dead read:
  - Resume FSM 종속 (`resumeMode/chainStateTracker/interviewPlanCache/resumeOrderCounter/playgroundCumulativeLength`) → FSM 폐기로 dead
  - `coveredClaims/coveredClaimsSet/activeChain/playgroundTurns` → `SessionStateLayer` 폐기로 dead
  - `compactedDialogueSummaries/compactionInFlight` → `DialogueCompactor` 폐기로 dead
  - `turnAnalysisCache: Map<Long, TurnAnalysis>` + `recordAnalysis` + `getAnswerAnalysis` = 이미 write-only dead cache (read 프로덕션 0)
  - `currentLevel` → `SessionStateLayer.toSessionStateSnapshot()` 단일 read consumer 폐기로 dead. prompt level hint = `Interview.userLevel` 직접 조회
  - `startedAt` → `ClockWatcher` 단일 read consumer 폐기로 dead. hard timeout backstop = 표준 트랙 동일 (FE terminate 신호 의존)
  - `resumeSkeletonCache` → `ResumeSkeletonRuntimeCache` wrapper read. wrapper / cache 둘 다 폐기 결정 (DB 재조회 ~5ms 대비 LLM ~300ms 미미). consumer 모두 `ResumeSkeletonPersister.findByInterviewId(id)` 직접 호출
  - **결론**: `InterviewRuntimeState` + `InterviewRuntimeStateCache` 두 클래스 전면 폐기 가능
- **ResumeSkeletonRuntimeCache**: wrapper. 본 plan 후 폐기 (consumer 가 `ResumeSkeletonPersister` 직접 주입)
- **TurnAnalysis interface**: `domain/interview/entity/TurnAnalysis.java` — 메서드 1개 `turnId()` 마커. 유일 구현 = `AnswerAnalysis`. 다형성 사용 부재
- **DialogueCompactor**: `infra/ai/context/compaction/DialogueCompactor.java` + `DialogueHistoryLayer` 사용 + `compaction_summarizer` callType
- **다층 컨텍스트**: `InterviewContextBuilder` → `SessionStateLayer` (coveredClaims read) + `DialogueHistoryLayer` (compactor) + `FocusLayer` + `PreambleLayer`
- **Rubric 매핑**: `_mapping.yaml` (rules 6개 + default) + `RubricLoader.loadMapping()` / `MappingResult` / `MappingRule` / `RubricResolutionContext` / `RubricMappingFamily.resolve(ctx)` — YAML rule 기반 라우팅 (`resumeTrack` / `InterviewType` / `RubricCategory` 매칭). `always_apply: nonverbal-v1` cross-cutting

### 사용자 결정 근거
- "기존 RESUME_PLAYGROUND / RESUME_INTERROGATION 도 제거하고 RESUME_MAIN / RESUME_FOLLOWUP 추가" — 레거시 enum 도 정리 (dev only truncate)
- "N = duration / 3 + 2" — main 1개 (질문 + 답변 + follow-up) 평균 3분 가정 + 2개 버퍼
- "이력서 트랙 단순화 = 표준 트랙 흡수. opener → main 끝. 이력서 맥락 = ResumeSkeleton 을 꼬리질문 prompt 에 동봉" — `selectedAnswerFeedbackPerspective` / `AnswerFeedbackPerspective` 즉시 제거 + `RecommendedNextAction.SKIP` 분기 유지 (표준 트랙 동일 패턴 흡수) 결정 근거
- "selectedAnswerFeedbackPerspective" — FE grep 결과 사용처 0. 백엔드 String 매핑이라 enum 타입 의존 0. 즉시 제거 안전
- "Rubric 매핑 _mapping.yaml 폐기 + QuestionType-driven Java switch" — 사용자 결정. weight / level_expectations 유지 (LLM 채점 품질 보존). 라우팅 룰만 Java 이동 → YAML 추상화 layer 1개 감소
- "turn 개념 전면 폐기. 이전 대화 = previousExchanges (FE 송신, main 부터 묶음). backend RuntimeState 미보유" — 사용자 결정. write-only dead cache `turnAnalysisCache` + `TurnAnalysis` interface + `AnswerAnalysis.turnId` 모두 제거. backend stateless = 표준 트랙 동일 패턴. 동시성 위험 0
- "RuntimeState 전면 폐기 + Skeleton 캐시도 폐기" — 사용자 결정. 모든 필드 dead read 확인 → `InterviewRuntimeState` + `InterviewRuntimeStateCache` + `ResumeSkeletonRuntimeCache` 3개 클래스 폐기. Skeleton 은 turn 마다 DB 재조회 (LLM ~300ms 대비 ~5ms 미미. YAGNI)
- "resumequestionplanner도 제거해도 되는거 아니야? interviewplan 제거되면" — 사용자 결정. ResumeQuestionPlanner 신규 클래스 미신설. 책임 = 기존 `ResumeTrackInitiator` 흡수 (skeleton ingest + main 일괄 LLM 1회 + saveResults). 표준 트랙 `StandardTrackQuestionGenerator` 와 대칭 패턴. 신규 클래스 0개
- "GPT-4o-mini primary 유지 + per-main 토큰 보수적 제한 + M max 40 clamp" — 사용자 정정. 프로젝트 AI Provider Stack (`AGENTS.md` Tech Stack: BE = GPT-4o-mini primary + Claude fallback) 준수. 응답 ~14.8K (16.4K 한계 대비 ~1.6K 안전 마진). chunk 분할 회피 사유 = chunk 경계 topic 중복 = Goal "동일 주제 3회 이상 차단" 직접 위협
- "LLM 실패 시 정책 = 전 트랙 throw 통일" — 사용자 결정 (#PR 481 리뷰 P1-2 반영). follow-up 생성 실패 (primary + fallback 모두 실패) = `BusinessException` 그대로 전파. FE 에서 운영자 알림 + 사용자 재시도 안내. 표준 / 이력서 트랙 동일. 기존 "aiSkip = true 응답으로 다음 main 진행" 정책 폐기 — LLM 실패와 의도된 skip (gap ≤ 1) 의미 혼재 방지. 면접 시작 시 (initiator bulk) 실패도 동일하게 면접 시작 자체 실패. `ResumeFallbackQuestions` / `ResumeFallbackBestAnswers` 두 hardcoded fallback 클래스 P3 폐기 (이력서 무관 fallback 질문 = 면접 품질 저하)

### 추정 / 미확인
- main 평균 소요 3분 가정 = 사용자 발화. 추후 운영 측정 기반 재조정 가능 (본 spec 비스코프)
- `applyL1FalseNegativeGuard` 가 interview 29 반복의 직접 원인이라는 명확한 trace 부재. Chain FSM `LEVEL_STAY` → 강제 `LEVEL_UP` (CHAIN_SWITCH 미작동) 이 1차 원인 — debugger-backend 분석 결과. L1FN 가드 폐기는 부수 정리
- `AskedPerspectives` / `SessionStateSnapshot` = JPA entity 가 아닌 record. 영속 테이블 부재. 폐기 = 클래스 제거만 (DROP TABLE 불필요)
- `Interview.durationMinutes` 범위 검증 부재 (`Integer`). N 공식 안전을 위해 spec 단 clamp 명시 (아래 §Data Model 8.5). **clamp 15~120 = 추정** — 면접 일반 범위. 서비스 정책 별도 확정 시 본 값 갱신 필요 (`InterviewService.createInterview` 검증 추가 권장 — 본 spec 비스코프)
- Rubric 매핑 룰 (5) `rubricCategory == EXPERIENCE → experience-technical-v1` 도달 조건 = `(InterviewType.TECH_FRONTEND/TECH_BACKEND/..., QuestionType with RubricCategory.EXPERIENCE)` 조합. 현 코드 grep 결과 `QuestionType` 중 `RubricCategory.EXPERIENCE` = `RESUME_OPENER` / `BEHAVIORAL_MAIN` / `BEHAVIORAL_FOLLOWUP` 만 → 룰 (1) `RESUME_BASED` 와 룰 (4) `BEHAVIORAL` 가 먼저 매칭 = 룰 (5) 도달 케이스 0. **P1 implement 시 backend agent 가 dev DB `question_score` row grep → 도달 row 0 확정 시 룰 (5) 폐기, 그 외 = 사용자 결정 요청**
- `Interview.durationMinutes` 분포 = clamp 15~120 추정 근거. implement 단 dev DB `SELECT MIN/MAX/AVG(duration_minutes) FROM interview` grep 으로 실제 분포 ⊆ [15, 120] 확정. 분포 외 row 발견 시 본 spec §Data Model 8.5 clamp 값 재조정 필요

## Trade-offs

### Option A (채택): QuestionType — 레거시 RESUME_PLAYGROUND / RESUME_INTERROGATION 폐기 + RESUME_MAIN / RESUME_FOLLOWUP 신설
- 장점:
  - question row 1개 조회로 "사전 / 꼬리 + 이력서 트랙" 식별 즉시 가능 (product-spec AC 3번 직결)
  - 레거시 enum 잔존 시 `isResume()` 분기에 4개 값 처리 부담 vs 3개 (`RESUME_OPENER / RESUME_MAIN / RESUME_FOLLOWUP`) 로 정리
  - `RubricCategory` 매핑 자유도 확보 (`RESUME_MAIN` = `TECHNICAL`, `RESUME_OPENER` = `EXPERIENCE` 유지, `RESUME_FOLLOWUP` = `TECHNICAL`)
- 단점: dev DB `questions.question_type` 컬럼 기존 `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` row truncate 필요
- 채택 사유: 사용자 결정 + AC 3번 (트랙 식별) 가장 직접적 충족. dev only 라 truncate 부담 0

### Option B (폐기): TECH_MAIN / TECH_FOLLOWUP 재사용
- 장점: enum 추가 0 / 표준 트랙 코드 경로 100% 재사용
- 폐기 사유: question row 단독 조회로 트랙 식별 불가 → interview.types join 필수 → AC 3번 위배 + 운영 추적성 저하

### Option C (폐기): 레거시 enum 유지 + RESUME_MAIN / RESUME_FOLLOWUP 만 추가
- 장점: 기존 데이터 backward compat (실제 의미 없음 — dev only)
- 폐기 사유: 사용자 결정 = 레거시 정리. 사용처 없는 enum 잔존 = 컨벤션 (`simplicity.md`) 위배

### Option D (채택): Resume 질문 생성자 — 기존 `ResumeTrackInitiator` 재활용 + 책임 단순화
- 장점:
  - 표준 트랙 `StandardTrackQuestionGenerator` 와 대칭 패턴 (`QuestionGenerationService` 진입점 동일 구조)
  - 신규 클래스 0개 → 컨벤션 `simplicity.md` 준수 (1회용 추상화 회피)
  - `QuestionGenerationService.generateQuestions` line 분기 (기존 코드) 변경 불요 → 회귀 위험 ↓
- 단점: 책임 변경 (Plan 준비 + RuntimeState seed + Orchestrator startSession 3개 → skeleton ingest + LLM 1회 + saveResults 3개) = 클래스 의미 전면 재작성 ≒ 신규 작성 비용
- 채택 사유: 사용자 결정. "InterviewPlan 제거되면 planner 도 불필요" 인사이트 + 진입점 대칭 패턴 + 신규 추상화 회피

### Option E (폐기): `ResumeQuestionPlanner` 신규 클래스 신설
- 장점: "planner" 의미가 신규 흐름과 정합 (계획 책임)
- 폐기 사유: planner = 다단계 / Chain / 분기 계획 의미 → 신규 흐름 (LLM 1회 일괄 생성) 에 과한 추상화. ResumeTrackInitiator 흡수가 단순

## Architecture

### Post 시퀀스 (BE 단독)

```
[Client] → InterviewController → InterviewService
   ↓ (status = IN_PROGRESS 전환 시)
   QuestionGenerationRequestedEvent
   ↓ (@TransactionalEventListener AFTER_COMMIT)
   QuestionGenerationService.generateQuestions (RESUME_BASED 분기)
   ↓
   ResumeTrackInitiator.initiate (재활용 — 책임 단순화)
       → 1) ResumeIngestionService.ingestExtractedText (skeleton 추출 + persist, 기존 유지)
       → 2) LLM 1회 호출 (call_type=resume_question_generator, GPT-4o-mini primary + Claude fallback) — opener N개 + main M개 일괄 생성
            ※ M = clamp(durationMinutes/3+2, 7, 40). 응답 ~14.8K (per-main 토큰 보수 제한)
            ※ LLM 실패 (primary + fallback 모두 실패) = BusinessException → 면접 시작 실패 + 운영자 알림 + 사용자 알림 (`InterviewStatus.FAILED` 전환)
       → 3) QuestionGenerationTransactionHandler.saveResults — QuestionSet 적재 (RESUME_OPENER × N + RESUME_MAIN × M, orderIndex 순)
   ↓
   (ResumeQuestionPlanner 신규 클래스 부재. ResumePlanPreparationService / PreparedResume / InterviewPlan / RuntimeState seed 의존 모두 제거)

[Client] → FollowUpController → FollowUpService.generateFollowUp
   ↓
   FollowUpContext load (current main question 식별)
   ↓
   AudioTurnAnalyzer.analyze
       → AnswerAnalysis 직접 반환 (new model: dimensionGaps + weakestDimension + claims)
       → TurnAnalysisResult / TurnAnalysisPipeline wrap 폐기 (turn 개념 전면 제거)
       → applyL1FalseNegativeGuard 호출 없음 (제거)
   ↓ recommendedNextAction == SKIP ?
   ├─ Yes → handleAnalyzerSkip (기존 분기 유지, 표준 트랙 동일 패턴)
   │        → FollowUpResponse.aiSkip + FollowUpQuestionCreatedEvent.of(...) 발행 → 종료
   │        ※ LLM 자율 결정 안전망 (답변 이탈 / 시간 만료 임박 케이스). product-spec Goal "꼬리질문 결정 코드 분기 0" 의 의도 (Chain FSM 깊이 강제 폐기) 와 별개 책임으로 유지
   └─ No (DEEPENING / CLARIFICATION) ↓
   StandardFollowUpPolicy.assertCanContinue
       → main = RESUME_OPENER 이면 follow-up 생성 skip (별도 분기 추가)
       → main = RESUME_MAIN 이면 follow-up 1회까지 허용
   ↓
   FollowUpQuestionWriter.write (RESUME_MAIN 직후만 호출)
       → prompt 입력 = 직전 main 질문 텍스트 + 사용자 답변 + AnswerAnalysis(weakestDimension/dimensionGaps/claims) + ResumeSkeleton
       → ResumeSkeleton 조회 = ResumeSkeletonPersister.findByInterviewId(id).orElseThrow() (DB 직접, 캐시 미경유)
       → DialogueHistoryLayer / SessionStateLayer 미참여 (폐기)
       → RESUME_FOLLOWUP row 생성
       → LLM 실패 시 (primary + fallback 모두 실패) = `BusinessException` 그대로 전파 (표준 트랙 동일). FE 에서 운영자 알림 + 사용자 재시도 안내. 의도된 LLM skip (gap ≤ 1 → skip=true) 만 `FollowUpResponse.aiSkip` 응답
   ↓
   FollowUpTransactionHandler.saveFollowUpResultAndPublishEvent
       → FollowUpQuestionCreatedEvent.of(...) (resumeMode/chainLevel/skeleton 필드 부재 — 표준 트랙과 동일 스키마)
   ↓ (@TransactionalEventListener AFTER_COMMIT, async)
   RubricScoringEventListener.on
       → RubricScorer.score (mode/level/skeleton 인자 제거)
       → QuestionScorePersister.saveRubric
```

`RecommendedNextAction` enum 자체는 유지 (`CLARIFICATION` / `DEEPENING` / `SKIP`). LLM 이 자율 결정. `FollowUpService.java:76` SKIP 분기 = 표준 트랙과 동일 패턴이라 본 plan 비변경.

### 폐기 컴포넌트 그래프

```
ResumeInterviewOrchestrator (256줄)
├─ PlaygroundModeHandler (158줄)
├─ InterrogationModeHandler (147줄)
├─ ChainStateTracker (126줄)
├─ ResumeModeTransitionPolicy
├─ ClockWatcher (resume 트랙 한정 사용. 표준 트랙 hard timeout 별도)
├─ ResumeInterviewPlanner (Chain 계획 — 폐기. main 일괄 생성 책임 = ResumeTrackInitiator 흡수)
├─ ResumePlanPreparationService (InterviewPlan 준비 — 폐기. ResumeIngestionService 만 잔존)
├─ PreparedResume (skeleton + plan wrapper — 폐기. ResumeIngestionService 가 skeleton 직접 반환)
├─ InterviewPlan / InterviewPlanPersister / InterviewPlanRuntimeCache / ProjectPlan / ProjectPlanListJsonConverter
├─ ResumeReplanLoader (재계획 — 신규 흐름엔 재계획 개념 없음)
├─ ResumeQuestionResultGenerator (Chain 기반 결과 — 사용 불요)
├─ ResumeTurnEventPublisher (TurnCompletedEvent.ofResumeTrack 호출 — FollowUpQuestionCreatedEvent.of 단일화)
├─ ResumeTrackPolicy (no-op 정책 — StandardFollowUpPolicy 단일화)
└─ InterviewTurnPolicyResolver (정책 분기 — 단일 정책으로 불요. RESUME 분기는 StandardFollowUpPolicy 내부에서)

분석 / 컨텍스트:
├─ AnswerFeedbackPerspective enum (7종, `domain/interview/entity/`)
├─ AskedPerspectives (record, JPA entity 아님 — DROP TABLE 불요)
├─ DialogueCompactor + compaction_summarizer callType
├─ DialogueHistoryLayer (다층 컨텍스트 layer)
├─ SessionStateLayer (coveredClaims read 단일 consumer)
├─ SessionStateSnapshot (record, JPA entity 아님 — DROP TABLE 불요)
├─ AnswerAnalysis.applyL1FalseNegativeGuard() 메서드 + 호출 2곳
├─ ChainStateTrackerSnapshot (`domain/resume/service/ChainStateTrackerSnapshot.java` — ChainStateTracker 폐기 동행)
├─ SkeletonCallType.RESUME_PLAYGROUND_OPENER / RESUME_PLAYGROUND_RESPONDER (`infra/ai/context/layer/SkeletonCallType.java` — playground / interrogation prompt 진입점)
├─ FocusLayer.CAP_RESUME_PLAYGROUND_OPENER / CAP_RESUME_PLAYGROUND_RESPONDER + FocusHints.ResumePlaygroundOpenerHints / ResumePlaygroundResponderHints (FocusLayer 본체는 잔존, 본 분기만 폐기)
├─ FollowUpResponse.selectedAnswerFeedbackPerspective 필드 (`domain/interview/dto/FollowUpResponse.java:24`)
├─ FollowUpRequest.FollowUpExchange.selectedAnswerFeedbackPerspective 필드 (`domain/interview/dto/FollowUpRequest.java:36`)
├─ GeneratedFollowUp.selectedAnswerFeedbackPerspective 필드 (`infra/ai/dto/GeneratedFollowUp.java:17`)
├─ GeneratedAnswerAnalysis.missingPerspectives 필드 (`infra/ai/dto/GeneratedAnswerAnalysis.java:16`)
├─ AnswerAnalysisJsonRenderer 의 askedPerspectives 인자 (`infra/ai/context/AnswerAnalysisJsonRenderer.java`)
├─ PromptFormatters.formatPerspectives (`infra/ai/prompt/PromptFormatters.java`)
└─ AudioTurnAnalyzerPromptBuilder / AnswerAnalyzerPromptBuilder / FollowUpPromptBuilder 의 askedPerspectives 인자

RuntimeState 전면 폐기:
├─ InterviewRuntimeState (13 필드 모두 dead read — 분석 §Evidence 참조)
├─ InterviewRuntimeStateCache (cache wrapper — consumer 0)
└─ ResumeSkeletonRuntimeCache (wrapper — consumer 가 ResumeSkeletonPersister 직접 주입)

분석 마커:
├─ TurnAnalysis (interface, 메서드 1개 = turnId getter — 다형성 사용 0)
├─ TurnAnalysisResult (record, answerText + AnswerAnalysis wrap — answerText 호출자 입력값 = wrap 불요)
├─ TurnAnalysisPipeline (서비스, TextFallbackTurnAnalyzer 위임 — 위임 1계층 = 불요)
├─ AnswerAnalysis.turnId (필드 — 캐시 key 용도였으나 캐시 폐기 시 의미 상실)
└─ InterviewRuntimeState.recordAnalysis / getAnswerAnalysis (write-only / read 부재)

FocusLayer 잔존 정리:
├─ FocusHints.currentLevel (int 필드 — Chain FSM 의존 잔재. chain 폐기 시 dead 파라미터)
├─ FocusHints.answerQuality (int 필드 — AnswerAnalysis.answerQuality 폐기 동행)
└─ FocusLayer.CURRENT_LEVEL prompt 라인 (chain level 표시 — chain 폐기 시 무의미)
```

## Data Model

### 1. QuestionType enum (Java)

```java
public enum QuestionType {
    TECH_MAIN(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    TECH_FOLLOWUP(ReferenceType.MODEL_ANSWER, RubricCategory.TECHNICAL),
    BEHAVIORAL_MAIN(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    BEHAVIORAL_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.BEHAVIORAL),
    RESUME_OPENER(ReferenceType.GUIDE, RubricCategory.EXPERIENCE),
    RESUME_MAIN(ReferenceType.GUIDE, RubricCategory.TECHNICAL),       // NEW
    RESUME_FOLLOWUP(ReferenceType.GUIDE, RubricCategory.TECHNICAL);   // NEW
    // 제거: RESUME_PLAYGROUND, RESUME_INTERROGATION

    public boolean isMain() {
        return this == TECH_MAIN || this == BEHAVIORAL_MAIN || this == RESUME_MAIN;
    }
    public boolean isFollowUp() {
        return this == TECH_FOLLOWUP || this == BEHAVIORAL_FOLLOWUP || this == RESUME_FOLLOWUP;
    }
    public boolean isResume() {
        return this == RESUME_OPENER || this == RESUME_MAIN || this == RESUME_FOLLOWUP;
    }
}
```

### 2. AnswerAnalysis (record)

```java
public record AnswerAnalysis(
        List<Claim> claims,
        Map<String, Integer> dimensionGaps,
        String weakestDimension,
        List<String> unstatedAssumptions,
        RecommendedNextAction recommendedNextAction
) {

    public AnswerAnalysis {
        claims = claims != null ? List.copyOf(claims) : List.of();
        dimensionGaps = dimensionGaps != null ? Map.copyOf(dimensionGaps) : Map.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public static AnswerAnalysis empty() { ... }
    // applyL1FalseNegativeGuard() 제거
    // missingPerspectives / answerQuality / turnId / mainQuestionId / withMainQuestionId / withTurnId 제거
    // TurnAnalysis interface implements 제거 — interface 자체 폐기
}
```

- `dimensionGaps` 키 = `RubricDimension.id` (String). LLM 응답에서 dimension id → gap (0~3, 0=완전 / 3=부재) Map 으로 받음
- `weakestDimension` = `dimensionGaps` 중 max gap 의 dimension id. tie 시 첫 dimension. LLM 이 자체 결정
- 추적 식별자 (`mainQuestionId`) = record 외부 호출자 (handler / event) 가 별도 변수로 보유. 분석 결과 record 안 중복 폐기 (호출자 path = handler → FollowUpQuestionWriter 가 main `Question` 객체 직접 보유)

### 3. FollowUpQuestionCreatedEvent (record) — TurnCompletedEvent 재명명 + 슬림

```java
public record FollowUpQuestionCreatedEvent(
        Long interviewId,
        Long userId,
        Long questionId,
        Long questionSetId,
        String userAnswer,
        AnswerAnalysis analysis,
        InterviewLevel userLevel
) {
    public static FollowUpQuestionCreatedEvent of(
            Long interviewId, Long userId,
            Long questionId, Long questionSetId,
            String userAnswer, AnswerAnalysis analysis,
            InterviewLevel userLevel
    ) {
        return new FollowUpQuestionCreatedEvent(...);
    }
}
```

**변경 요약**:
- 이름: `TurnCompletedEvent` → `FollowUpQuestionCreatedEvent`. 발행 시점 = follow-up question row 저장 직후 → "follow-up 질문 row 생성됨" 상태 알림 (`backend/.claude/rules/conventions.md` §Event "자기 상태 알림 전용" 룰 정합). "Turn" 용어 모호 (opener / main / followup 어느 단위?) 해소. 이름 = 발행 시점 (follow-up only) 정확 반영 — opener / main 일괄 생성은 본 이벤트 비대상
- `turnIndex` 필드 폐기 — 사용처 = `RubricScoringEventListener` 로그 / 예외 메시지만 (비즈니스 분기 0). `questionId` + DB 조회 시 `Question.orderIndex` 로 대체 충분
- `resumeMode` / `currentChainLevel` / `resumeSkeleton` nullable 필드 폐기 (Chain FSM / Mode 폐기 동행)
- `ofStandard` / `ofResumeTrack` 정적 팩토리 → `of(...)` 단일
- 발행 위치 = `FollowUpTransactionHandler.publishFollowUpQuestionCreatedEvent` (기존 `publishTurnCompletedEvent` 재명명). opener / main planner 일괄 생성은 이벤트 발행 X (Rubric 채점 트리거 = 답변 동봉 필요 → follow-up 시점만 발행)
- 소비처 = `RubricScoringEventListener.on(FollowUpQuestionCreatedEvent)` (기존 시그니처 변경)

### 4. Rubric.selectDimensions (메서드 시그니처)

```java
// Before
public List<String> selectDimensions(ResumeMode resumeMode) { ... }

// After
public List<String> selectDimensions() {
    List<String> answerDims = perTurnRules.get("on_intent_answer");
    if (answerDims != null) return answerDims;
    return usesDimensions.stream().map(DimensionRef::ref).toList();
}
```

- `perTurnRules` key 정리: `on_playground_mode`, `on_interrogation_mode` 제거 (YAML 파일도 동일)

### 5. RubricScorer.score (메서드 시그니처)

```java
// Before: score(question, questionSet, interview, userAnswer, analysis, resumeMode, currentChainLevel, resumeSkeleton)
// After:
public RubricScoringResult score(
        Question question, QuestionSet questionSet, Interview interview,
        String userAnswer, AnswerAnalysis analysis
) { ... }
```

### 6. ResumeSkeleton (record)

```java
public record ResumeSkeleton(
        String resumeId,
        String fileHash,
        CandidateLevel candidateLevel,
        String targetDomain,
        List<Project> projects
        // interrogationPriorityMap 제거
) {
    // priorityIds(String) 메서드 제거
}
```

### 7. InterviewRuntimeState / InterviewRuntimeStateCache / ResumeSkeletonRuntimeCache 전면 폐기

3 클래스 동시 폐기. 모든 필드 dead read 분석 §Evidence 참조.

**Skeleton 조회 — 매 turn DB 재조회**:
- `ResumeTrackInitiator` (면접 시작 1회) → `ResumeSkeletonPersister.findByInterviewId(id)`
- `FollowUpQuestionWriter` (각 follow-up turn) → `ResumeSkeletonPersister.findByInterviewId(id)`
- `RubricScorerPromptBuilder` (각 채점 이벤트) → `ResumeSkeletonPersister.findByInterviewId(id)` (현재는 `RubricScorer.score` 시그니처로 전달받음 — task 2 시그니처 정리로 자체 주입 전환)
- 비용: LLM 호출 ~300ms 대비 DB ~5ms = ROI 미미. 캐시 추가 필요 시 future 작업

**이전 답변 맥락 보존 = backend stateless** (표준 트랙 패턴):
- FE 가 매 follow-up 요청 시 `FollowUpRequest.previousExchanges: List<FollowUpExchange>` 송신
- main 1개 + 그 main 의 follow-up 들 묶음. 다음 main 시작 시 FE 가 reset
- 이력서 트랙 = main 당 follow-up 1개 정책 → previousExchanges 최대 1 entry
- backend 가 별도 저장 안 함 = RuntimeState 무보유 + 무상태 단순성 + 동시성 위험 0

**user level prompt hint**:
- 표준 트랙 동일하게 `Interview.userLevel` 직접 조회 (`FollowUpGenerationRequest.level` 으로 전달). `currentLevel` 캐시 필드 불요

**hard timeout**:
- `ClockWatcher` 폐기. FE terminate 신호 단일 종료 경로 (표준 트랙 동일). backend backstop 부재

### 8. Flyway 마이그레이션 (V48)

```sql
-- V48__drop_interview_plan.sql
DROP TABLE IF EXISTS interview_plan;
```

**Flyway 본문 = DDL 1개**. `backend/.claude/rules/conventions.md` §Flyway "DDL 전용. DML 금지" 준수. JSON cleanup / questions row truncate 등 DML 은 본 파일에 주석으로도 포함하지 않음 (운영자 오해 방지).

`AskedPerspectives` / `SessionStateSnapshot` / `ChainStateTrackerSnapshot` = record. JPA entity 아님 (`@Entity` / `@Table` 부재 grep 확인). 영속 테이블 부재 → DROP TABLE 불필요. 폐기 = 클래스 파일 제거만.

### 8.1. 운영 SQL (dev only — Flyway 분리)

`scripts/dev-cleanup-resume-legacy.sql` (P2 작업 14 신규):

```sql
-- dev only. P2 머지 직전 수동 실행 (Flyway 자동 실행 X)
-- 1) 레거시 question_type row 삭제 (FK 순서 준수)
DELETE FROM question_score
 WHERE question_id IN (SELECT id FROM questions WHERE question_type IN ('RESUME_PLAYGROUND', 'RESUME_INTERROGATION'));
DELETE FROM questions WHERE question_type IN ('RESUME_PLAYGROUND', 'RESUME_INTERROGATION');

-- 2) 이력서 트랙 기존 채점 결과 truncate (Rubric.selectDimensions 변경 → 신규 dimension 축 재해석 vs 폐기)
--    Rubric YAML mode key (on_playground_mode / on_interrogation_mode) 제거로 기존 score row 가 신규 dimension 축과 불일치
--    dev only → 폐기 선택. 운영 truncate
DELETE FROM question_score
 WHERE question_id IN (
   SELECT q.id FROM questions q
   JOIN question_set qs ON q.question_set_id = qs.id
   WHERE qs.category = 'RESUME_BASED'
 );

-- 3) resume_skeleton JSON 의 interrogation_priority_map key 정리 (역직렬화 호환 강화 목적)
--    @JsonIgnoreProperties(ignoreUnknown = true) 적용되어 있어 미실행 시에도 안전. 정리는 보강 차원
UPDATE resume_skeleton SET payload = JSON_REMOVE(payload, '$.interrogationPriorityMap')
 WHERE JSON_EXTRACT(payload, '$.interrogationPriorityMap') IS NOT NULL;
```

테이블 / 컬럼명 (`question_score` / `question_set.category` / `resume_skeleton.payload`) = implement 단 grep 확인 후 정정.

### 8.5. durationMinutes clamp

`Interview.durationMinutes` 검증 부재 → `ResumeTrackInitiator` 내 clamp:

```java
int durationMinutes = Math.max(15, Math.min(120, interview.getDurationMinutes()));
int mainCount = Math.min(40, durationMinutes / 3 + 2);   // 7 ~ 40 사이
```

- min 15분 / max 120분 — 면접 일반 범위 추정 (서비스 정책 별도 확정 시 본 값 갱신)
- mainCount min(40, ...) clamp = 토큰 한계 안전 마진
- 토큰 추정: `ResumeTrackInitiator` LLM 호출 응답 = main M개 × (질문 + bestAnswer + topic) ≈ M × 400 token. M=40 → ~16.0K
- 채택: **GPT-4o-mini primary 유지 + Claude fallback** (프로젝트 AI Provider Stack 컨벤션 준수 — `AGENTS.md` Tech Stack 명시). 응답 토큰 한계 회피책 = **per-main 토큰 보수적 제한** (질문 50 토큰 + topic 20 토큰 + bestAnswer 300 토큰 = ~370 / main. M=40 → ~14.8K) + **M max 40 clamp** (durationMinutes max 120 → mainCount = duration/3 + 2 → min(40, durationMinutes/3 + 2)). chunk 분할 회피 사유 = chunk 경계 topic 중복 위험
- 호출 callType = `resume_question_generator` (planner 가 아닌 generator 명명 — 표준 트랙 동등). `ResilientAiClient` 의 callType 별 primary/fallback 모델 매핑 = 표준 트랙 동일 (GPT-4o-mini primary + Claude fallback)
- **LLM 호출 실패 정책**:
  - (1) **면접 시작 시 (initiator bulk 1회 호출)**: GPT-4o-mini + Claude fallback 모두 실패 시 = 면접 시작 실패 (사용자 알림 + 운영자 알림). 부분 응답 (truncate `finish_reason=length` / parse 실패) 적재 금지 — `AiResponseParser.parseOrRetry` 의 schema hint retry 후 실패 시 동일 처리
  - (2) **면접 진행 중 (per-turn follow-up 생성)**: LLM 호출 실패 (primary + fallback 모두 실패) 시 = `BusinessException` 그대로 전파 (사용자 결정 — PR 481 리뷰 P1-2 반영. aiSkip 으로 다음 main 진행 정책 폐기). 전 트랙 (표준 / 이력서) 동일. FE 에서 사용자 재시도 안내 + 운영자 알림. 의도된 LLM skip (prompt 가 `skip=true` 반환 — gap ≤ 1) 만 `FollowUpResponse.aiSkip` 응답으로 다음 main 진행
- 결과: mainCount 범위 = 7 (15분) ~ 40 (120분). 응답 토큰 ~14.8K 이내 GPT-4o-mini 한계 16.4K 안전 마진 확보 (~1.6K)

## API Contract

`POST /api/v1/interviews/{id}/follow-up` 응답 schema 변경 = `selectedAnswerFeedbackPerspective` 필드 제거.

### Before
```json
{
  "questionId": 123,
  "question": "...",
  "selectedAnswerFeedbackPerspective": "DEPTH",  // 제거
  "type": "...",
  ...
}
```

### After
```json
{
  "questionId": 123,
  "question": "...",
  "type": "...",
  ...
}
```

### FE 영향 — 분리 작업 필수

**`selectedAnswerFeedbackPerspective` / `missingPerspectives` / `answerQuality`**: FE grep 0 → BE 단독 제거 가능.

**`RESUME_PLAYGROUND` / `RESUME_INTERROGATION` → `RESUME_MAIN` / `RESUME_FOLLOWUP`**: FE grep 9건. BE+FE 동시 작업:
- `frontend/src/types/interview.ts:63-64` — `QuestionType` literal union 갱신 (5 → 6 멤버, RESUME_OPENER + RESUME_MAIN + RESUME_FOLLOWUP + 표준 트랙 3)
- `frontend/src/utils/question-type.ts:6` — `isMain` / `isFollowup` 로직 분기 갱신 (`RESUME_MAIN` 추가 / `RESUME_FOLLOWUP` 추가)
- `frontend/src/utils/__tests__/question-type.test.ts:30,31,51` — 테스트 fixture 갱신
- `frontend/src/components/feedback/__tests__/content-tab.test.tsx:56,125,135,150` — 테스트 fixture 갱신
- `frontend/src/components/feedback/feedback-panel.tsx:14-15` — label map 갱신 (`RESUME_MAIN` / `RESUME_FOLLOWUP` 한국어 label 명시 필요. UI/UX 결정 항목 = `frontend/.claude/rules/conventions.md` 참고)

### 요청 측 (`FollowUpRequest.FollowUpExchange.selectedAnswerFeedbackPerspective`)
- 동시 제거. FE 가 본 필드 송신 안 함 (grep 0). request body 에 잔존해도 무시되나 schema 정합 위해 제거.

## Verification

- [ ] **Service Integration**: `ResumeTrackInitiatorIntegrationTest` — 이력서 면접 시작 시 opener N + main M (=duration/3+2) 일괄 생성. RESUME_OPENER 모두 EXPERIENCE 카테고리, RESUME_MAIN 모두 TECHNICAL, ResumeSkeleton.projects 범위 내 topic. LLM mock fixture = `src/test/resources/fixtures/resume-question-generator-response.json` (JSON schema: `{"openers":[{"questionText":..., "ttsText":...}], "mains":[{"topic":..., "questionText":..., "bestAnswer":...}]}`)
- [ ] **Service Integration**: `FollowUpServiceResumeFlowTest` — RESUME_OPENER 응답 후 follow-up row 미생성 / RESUME_MAIN 응답 후 RESUME_FOLLOWUP row 1개 생성 / orderIndex 순서 일관성
- [ ] **Domain Unit**: `AnswerAnalysisTest` — 신규 record 생성 / `applyL1FalseNegativeGuard` 메서드 부재 (컴파일 검증)
- [ ] **Domain Unit**: `RubricTest` — `selectDimensions()` 무인자 호출. resumeMode 인자 메서드 부재
- [ ] **Infra Integration**: `AnswerAnalyzerPromptRenderingTest` — prompt template 변경 후 `missing_perspectives` 토큰 부재 + `dimension_gaps` / `weakest_dimension` 토큰 포함 + askedPerspectives 인자 부재 확인 (mock LLM 응답 schema = `{"claims":[...], "dimension_gaps":{...}, "weakest_dimension":"...", "unstated_assumptions":[...], "recommended_next_action":"..."}`)
- [ ] **E2E**: `ResumeInterviewE2ETest` — `/interviews` 생성 → `/interviews/{id}/status` IN_PROGRESS → opener 응답 → main 응답 (`/follow-up`) → RESUME_FOLLOWUP 응답 → 시간 만료 종료. 성공 1 케이스
- [ ] **회귀 (`@Disabled`)**: `ResumeRepetitionRegressionTest` — interview 29 답변 패턴 (동일 topic 반복 답변) 입력 시 동일 topic 3회 이상 출제 발생 시 fail. `@EnabledIfEnvironmentVariable(name="RUN_LIVE_API", matches="true")`
- [ ] **Rubric snapshot diff**: 기존 fixture (BEHAVIORAL / TECH / EXPERIENCE 각 1) 점수 결과 변경 0
- [ ] **Rubric 매핑 회귀**: `RubricLoaderResolveTest` — 기존 `_mapping.yaml` 룰 6개 모두 동일 결과 (`(InterviewType.RESUME_BASED, *) → resume-v1` / `(CS_FUNDAMENTAL, *) → concept-cs-fundamental-v1` / `(LANGUAGE_FRAMEWORK, *) → concept-lang-framework-v1` / `(UI_FRAMEWORK, *) → concept-lang-framework-v1` / `(BEHAVIORAL, *) → experience-collaboration-v1` / `(*, TECH_MAIN/TECH_FOLLOWUP with EXPERIENCE category) → experience-technical-v1` / default `fallback-generic-v1`). `_mapping.yaml` 파일 부재 검증
- [ ] **Build**: `./gradlew build` 통과
- [ ] **grep 잔존 0**: §Architecture 폐기 컴포넌트 그래프 (L124-178) 의 모든 클래스명 / 메서드명 / 필드명 / prompt 토큰. 전체 목록 = §Appendix A (본 문서 말미). 메서드명 (`applyL1FalseNegativeGuard` / `recordAnalysis` / `getAnswerAnalysis` / `turnAnalysisCache`) + 필드명 (`FocusHints.currentLevel` / `FocusHints.answerQuality` / `missingPerspectives` / `answerQuality` / `turnId` / `selectedAnswerFeedbackPerspective`) + prompt 토큰 (`FocusLayer.CURRENT_LEVEL` / `missing_perspectives` / `_mapping.yaml`) + 파일명 (`InterviewRuntimeState.java` 등) 잔존 0
- [ ] **ArchUnit gate**: §Appendix A 의 모든 클래스 패키지 부재 단언 + `applyL1FalseNegativeGuard` 메서드명 사용 부재 + `TurnAnalysis` interface 부재 — `ResumeArchitectureTest` (아래) 흡수. grep 보조
- [ ] **운영 로그**: follow-up 생성 시 `weakestDimension=X target_claim_idx=Y` 노출 (`FollowUpService.generateAndSaveFollowUp` 로그)
- [ ] **회귀 영역 통합**: `FollowUpServiceTest` (CS 트랙) / `BehavioralFollowUpServiceTest` 기존 케이스 통과
- [ ] **JSON 역직렬화 호환**: `ResumeSkeletonJsonCompatibilityTest` — 기존 `interrogationPriorityMap` 필드 포함 JSON payload 입력 시 `@JsonIgnoreProperties(ignoreUnknown = true)` 에 의해 무시 + record 생성 성공
- [ ] **ArchUnit** (P3): `ResumeArchitectureTest` — §Appendix A 의 모든 클래스 부재 단언 + `applyL1FalseNegativeGuard` 메서드 부재 단언. CI 자동 가드
- [ ] **결정적 회귀** (P2): `ResumeRepetitionDeterministicTest` — interview 29 trace 기반 모킹 LLM 응답 시퀀스로 동일 topic 3회 이상 출제 발생 시 fail. Live LLM 미의존 → 비결정성 격리
- [ ] **보안 (로그 마스킹)**: `ResumeTrackInitiatorLoggingTest` — initiator LLM 호출 시 ResumeSkeleton 전체 payload 가 INFO 로그에 기록되지 않음 검증 (logback capture). INFO = `resumeId` / `call_type` / `fileHash` 만. payload 본문 = DEBUG 레벨 (운영 미적용)

## Pre / Post State

### Pre
```
domain/resume/ : 45 파일 / 2352줄
  ├─ entity/ (Chain*, ResumeMode, InterviewPlan, ProjectPlan, ResumeSkeleton with interrogationPriorityMap)
  └─ service/ (Orchestrator, PlaygroundHandler, InterrogationHandler, Planner, ReplanLoader, ...)

domain/question/service/ResumeTrackInitiator.java : prepare(InterviewPlan) + runtimeState seed + orchestrator startSession + saveResults(emptyList)
domain/resume/service/ResumeFallbackQuestions.java : hardcoded fallback 질문 제공
domain/resume/service/ResumeFallbackBestAnswers.java : hardcoded best answer 제공
domain/resume/service/ResumePlanPreparationService.java : InterviewPlan + ResumeSkeleton 동시 준비
domain/resume/service/PreparedResume.java : record (skeleton + plan wrapper)
domain/interview/entity/AnswerAnalysis.java : turnId + missingPerspectives + answerQuality + applyL1FalseNegativeGuard() + implements TurnAnalysis
domain/interview/entity/TurnAnalysis.java : marker interface (turnId getter)
domain/interview/entity/TurnAnalysisResult.java : record (answerText + AnswerAnalysis wrap)
domain/interview/service/TurnAnalysisPipeline.java : TextFallbackTurnAnalyzer 위임 1계층
domain/interview/entity/InterviewRuntimeState.java : 13 필드 (coveredClaims/chainStateTracker/turnAnalysisCache/...)
domain/interview/service/InterviewRuntimeStateCache.java : runtime state ConcurrentHashMap wrapper
domain/resume/service/ResumeSkeletonRuntimeCache.java : skeleton read-through cache
domain/feedback/rubric/entity/Rubric.java : selectDimensions(ResumeMode) mode 분기
domain/feedback/rubric/service/RubricScorer.java : score(..., ResumeMode, Integer, ResumeSkeleton)
domain/feedback/rubric/event/TurnCompletedEvent.java : 11 필드 (turnIndex/resumeMode/currentChainLevel/resumeSkeleton 포함)
resources/rubric/_mapping.yaml + RubricMappingFamily.resolve(RubricResolutionContext) : YAML rule 기반 매핑
infra/ai/context/compaction/DialogueCompactor.java
infra/ai/context/layer/ : SessionStateLayer + DialogueHistoryLayer + FocusLayer + PreambleLayer
infra/ai/context/layer/FocusHints.java : currentLevel:int + answerQuality:int 필드
infra/ai/context/layer/FocusLayer.java : CURRENT_LEVEL prompt 라인

domain/question/entity/QuestionType.java : RESUME_PLAYGROUND + RESUME_INTERROGATION 포함
```

### Post
```
domain/resume/ : 슬림 (Skeleton + 파싱 / 영구화만)
  └─ entity/Project.java, ResumeSkeleton.java (interrogationPriorityMap 제거)
  └─ service/ResumeIngestionService.java, ResumeExtractionService.java,
              ResumeSkeletonPersister.java
              (ResumePlanPreparationService / PreparedResume / ResumeSkeletonRuntimeCache / ResumeFallbackQuestions / ResumeFallbackBestAnswers 폐기)

domain/question/service/ResumeTrackInitiator.java : 책임 변경 — skeleton ingest + LLM 1회 (callType `resume_question_generator`, GPT-4o-mini primary + Claude fallback) main 일괄 생성 + saveResults
domain/interview/entity/AnswerAnalysis.java : 5 필드 (claims + dimensionGaps + weakestDimension + unstatedAssumptions + recommendedNextAction). turnId / TurnAnalysis implements 제거
domain/interview/entity/TurnAnalysis.java : 폐기
domain/interview/entity/TurnAnalysisResult.java : 폐기 (AnswerAnalysis 직접 반환)
domain/interview/service/TurnAnalysisPipeline.java : 폐기
domain/interview/entity/InterviewRuntimeState.java : 폐기
domain/interview/service/InterviewRuntimeStateCache.java : 폐기
domain/resume/service/ResumeSkeletonRuntimeCache.java : 폐기 (consumer = ResumeSkeletonPersister 직접 주입)
domain/feedback/rubric/entity/Rubric.java : selectDimensions() 무인자
domain/feedback/rubric/service/RubricScorer.java : score(question, questionSet, interview, userAnswer, analysis)
domain/feedback/rubric/event/FollowUpQuestionCreatedEvent.java : 7 필드 (TurnCompletedEvent 재명명. turnIndex + resume 3 필드 제거)
domain/feedback/rubric/service/RubricLoader.java : resolveRubricId(InterviewType, QuestionType) Java switch 룰. _mapping.yaml 폐기
infra/ai/context/ : DialogueCompactor / DialogueHistoryLayer / SessionStateLayer 폐기
infra/ai/context/layer/FocusHints.java : currentLevel + answerQuality 필드 제거
infra/ai/context/layer/FocusLayer.java : CURRENT_LEVEL prompt 라인 제거

domain/question/entity/QuestionType.java : RESUME_OPENER + RESUME_MAIN + RESUME_FOLLOWUP
```

## 비기능 요구 (NF 11)

| NF | 결정 | 근거 |
|---|---|---|
| 영향 범위 | BE 단독. FE / Lambda 0 | grep 0 (FE 사용 부재) + Lambda 미참여 |
| 정합성 | 트랜잭션적 (follow-up row 저장 + FollowUpQuestionCreatedEvent 발행 동일 트랜잭션 — 기존 표준 트랙 동일) | `FollowUpTransactionHandler.saveFollowUpResultAndPublishEvent` |
| 실시간성 | follow-up 생성 P95 < 5s (LLM 1회 + DB 저장). initiator = 면접 시작 시 1회 백그라운드 호출 — 사용자 대기 영향 0 | 표준 트랙 측정값 = 동일 범위 |
| 부하 | LLM 호출 빈도 감소 — 모드 전환 시마다 N회 → 면접 시작 1회 (initiator) + main 마다 follow-up 1회. dialogue compactor LLM 호출 (`compaction_summarizer`) 폐기. 전체 토큰 비용 ↓ | `compaction_summarizer` callType grep 후 폐기 동행 |
| 동시성 | Resume 트랙 단일 사용자 단일 세션. 동시 follow-up 호출 미해당 (FE 가 직렬 호출) | 기존 표준 트랙 동일 가정 |
| 마이그레이션 | Flyway V48 = DDL 1개. 운영 SQL 별도 (`scripts/dev-cleanup-resume-legacy.sql`). dev only → backfill 불요 | 본 spec §Data Model 8 + 8.1 |
| 외부 의존 | ResilientAiClient 적용. 신규 callType `resume_question_generator` = **GPT-4o-mini primary + Claude fallback** (프로젝트 AI Provider Stack 컨벤션 준수). 응답 토큰 안전 마진 = per-main 보수 토큰 제한 + M max 40 clamp. 표준 트랙 callType 매핑 동일 | `infra/ai/ResilientAiClient` 기존 fallback 패턴 + 본 spec §Data Model 8.5 + `AGENTS.md` Tech Stack |
| 보안 | LLM 호출 로그에 ResumeSkeleton **전체 payload 기록 금지**. `resumeId` / `fileHash` / `call_type` 만 기록. payload 본문은 trace_id 기반 별도 storage (현 trace 인프라 의존). 이력서 = 개인정보 (직무 / 프로젝트명 / 회사명 가능) → A09 (security logging failures) 준수 | `.claude/rules/security.md` A09 |
| 관찰성 | 신규 callType `resume_question_generator` AiCallMetrics 추가. 폐기 callType `compaction_summarizer` 메트릭 panel 제거 메모 = `docs/observability/dashboards.md` (P2 머지 시 운영자 노트). 운영 로그: follow-up 생성 시 `weakestDimension=X dimensionGaps={a:0,b:1,c:1} target_claim_idx=Y` 노출 (tie / 분포 추적 가능) | `infra/ai/metrics/AiCallMetrics` |
| 롤백 | feature flag 0. rollback = git revert + dev DB truncate. P1 / P2 / P3 phase 분리로 단계별 revert 가능 | 본 spec §롤백 시나리오 |
| 검증 | Service Integration + Domain Unit + E2E + Rubric snapshot + ArchUnit (P3 잔여) + grep | 본 spec §Verification |

## 위험 / 마이그레이션 / 롤백

### 위험
- **R1 — Rubric snapshot 회귀**: `selectDimensions(ResumeMode)` 폐기로 mode 별 dimension 분기 사라짐 → 이력서 트랙 채점 결과 변화 가능. **완화** = P1 단독 분리 + snapshot fixture diff 0 검증 강제
- **R2 — LLM 응답 schema 변경 회귀**: `AnswerAnalyzer` / `AudioTurnAnalyzer` LLM prompt 변경 → 응답 JSON schema 변경 (`missing_perspectives` → `dimension_gaps`). 응답 파싱 실패 가능. **완화** = `AiResponseParser.parseOrRetry` 의 schema hint retry + Service Integration 테스트 새 fixture
- **R3 — dev DB 의존**: `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` row truncate 필요. **완화** = dev only 명시, 운영 SQL 별도 스크립트 (Flyway DML 금지 룰)
- **R4 — 표준 트랙 시그니처 회귀**: `AudioTurnAnalyzer.analyze` 반환 타입 = `TurnAnalysisResult` → `AnswerAnalysis` 직접 + `SessionStateLayer` / `DialogueHistoryLayer` 폐기 → 표준 트랙 (CS / BEHAVIORAL) 호출자 (`FollowUpService:72,82,97` / `TextFallbackTurnAnalyzer:30,46`) 시그니처 변경 + context 구성 변경. **완화** = `FollowUpServiceTest` (CS 트랙) / `BehavioralFollowUpServiceTest` 전체 케이스 통과 강제 + grep 잔존 0 (`TurnAnalysisResult` / `TurnAnalysisPipeline`)
- **R5 — LLM 일관성 회복 측정 부재**: L1FN 가드 + DialogueCompactor + 다층 컨텍스트 폐기 후 LLM 이 동일 topic 반복 출제 안 하는지 결정성 의존. **완화** = `ResumeRepetitionDeterministicTest` (모킹 LLM 결정적) + Live `ResumeRepetitionRegressionTest` (참고). P2 머지 후 dev 운영 로그 모니터링 (`weakestDimension=` + `target_claim_idx=` 분포) 1주 관찰. **임계치**: dev 면접 sample 에서 동일 topic 3회 이상 발생 비율 < 1% = Goal 충족 / 1~5% = prompt fine-tune phase 추가 / ≥ 5% = prompt 재설계 분기. 임계 기준 = 사용자 결정 / agent 자율 조정 가능 (본 spec 비스코프)
- **R6 — Resume main 생성 LLM 응답 토큰 한계**: M 최대 40개 일괄 생성 시 응답 토큰 ~14.8K (보수 토큰 / main 제한 후). GPT-4o-mini 16.4K 한계 대비 ~1.6K 안전 마진. **완화** = (1) M clamp `min(40, durationMinutes/3 + 2)` + (2) per-main 보수 토큰 제한 (질문 50 + topic 20 + bestAnswer 300 ≈ 370) + (3) chunk 분할 회피 (topic 중복 위험). 실측 시 응답 초과 발생 = Claude fallback 도달, fallback 도 실패 시 면접 시작 실패 (운영자 알림). chunk 회피 사유 = chunk 경계 topic 중복 위험 자연 차단
- **R7 — 보안 (LLM 로그 개인정보 노출)**: ResumeSkeleton payload 에 사용자 직무 / 프로젝트명 포함. LLM 호출 logger 가 prompt 전체 기록 시 A09 위배. **완화** = `infra/ai/logging/` 마스킹 정책 점검 (기존 정책 grep 후 부재 시 P2 작업에 추가). `ResumeTrackInitiatorLoggingTest` 로 가드

### 마이그레이션 전략
- **P1 단독 분리** = Rubric 디커플링만 머지. snapshot diff 0 확인 후 P2 진입
- **P2 단일 PR** = 신규 흐름 + 분석 모델 + 컨텍스트 단순화 동시. dev DB truncate 동행
- **P3 단독** = Skeleton + RuntimeState 잔여 슬림화

### 롤백 시나리오
- feature flag 0 (dev only). rollback = git revert + dev DB truncate (questions / interviews / question_sets / answers / ...)
- P1 머지 후 snapshot diff 발생 시 즉시 revert + Rubric YAML 재검토

## 분기 결정

- [x] **BE+FE 분리 + BE 선행 강제 (강결합)** → `implement-be.md` + `implement-fe.md`
  - BE 단독 변경: `selectedAnswerFeedbackPerspective` / `missingPerspectives` / `answerQuality` 필드 제거 (FE grep 0)
  - **BE+FE 동시 변경**: `QuestionType` enum (`RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 폐기 + `RESUME_MAIN` / `RESUME_FOLLOWUP` 신설) — FE union 타입 + label + 분기 로직 갱신 9건
  - **순서**: BE P2 머지 → FE P2 PR 머지. BE 선행 사유 = QuestionType DB row 변경 + BE 응답 schema 가 source of truth. FE 단독 머지 시 BE 응답에 신규 enum literal 도달 가능성 0 → BE 후행 필수
  - 3 Phase 분리 = P1 / P2 / P3. P1 (BE only) → P2 (BE 선행 + FE 후행 동일 phase 윈도우 내) → P3 (BE only)
  - **P2 BE = 15 task**, **P2 FE = 5 task** (별도 PR). `docs/plans/AGENTS.md` §6 "Task 8개+ → tasks/ 분리" 룰 → P2 BE 만 `implement-be/tasks/p2-NN-*.md` 분리. FE / P1 / P3 단일 파일

### implement-fe.md 작업 범위 (P2 FE)

1. `QuestionType` literal union 갱신 (`types/interview.ts:63-64`) — `RESUME_PLAYGROUND` / `RESUME_INTERROGATION` 제거 + `RESUME_MAIN` / `RESUME_FOLLOWUP` 추가
2. `isMain` / `isFollowup` 분기 갱신 (`utils/question-type.ts`) — `RESUME_MAIN` → main / `RESUME_FOLLOWUP` → followup 분류
3. `feedback-panel.tsx` label map 갱신 — UI 표시 한국어 label 결정 (예: "이력서 질문" / "이력서 꼬리질문") — 디자인 사용자 자율 결정 권장 ([Autonomous Designer Mode] 메모리)
4. `selectedAnswerFeedbackPerspective` 송신 필드 제거 (`FollowUpRequest` payload 생성 코드 grep — 발견 시)
5. 테스트 fixture 갱신 (`utils/__tests__/question-type.test.ts`, `components/feedback/__tests__/content-tab.test.tsx`)

### Implement Phase Task 그룹 (요약)

**P1 — Rubric 디커플링** (단일 PR):
1. `Rubric.selectDimensions(ResumeMode)` → `selectDimensions()` 무인자 시그니처. `perTurnRules` mode key (`on_playground_mode`, `on_interrogation_mode`) 사용처 제거 + YAML 정리
2. `RubricScorer.score(...)` 시그니처 resumeMode/chainLevel/skeleton 3개 nullable 파라미터 제거
3. `RubricScoringEventListener.on` event.resumeMode/currentChainLevel/resumeSkeleton 참조 제거
4. `TurnCompletedEvent` → `FollowUpQuestionCreatedEvent` 재명명. `turnIndex` + resume 필드 3개 + `ofResumeTrack` 정적 팩토리 제거 → `of(...)` 단일. 발행 위치 `FollowUpTransactionHandler.publishFollowUpQuestionCreatedEvent`. 소비자 `RubricScoringEventListener.on(FollowUpQuestionCreatedEvent)`
5. **Rubric 매핑 Java 이동** — `_mapping.yaml` 폐기. `RubricLoader.loadMapping()` / `MappingResult` / `MappingRule` / `RubricResolutionContext` / `RubricMappingFamily.resolve(ctx)` 폐기. 신규 `RubricLoader.resolveRubricId(InterviewType, QuestionType)` private 메서드 (switch 룰). `resolveFor(question, questionSet, interview)` 시그니처 유지 (내부만 변경). `always_apply.nonverbal-v1` = 상수 `RubricLoader.ALWAYS_APPLIED_RUBRIC_IDS`. 매핑 룰 = (1) `RESUME_BASED → resume-v1` (2) `CS_FUNDAMENTAL → concept-cs-fundamental-v1` (3) `LANGUAGE_FRAMEWORK|UI_FRAMEWORK → concept-lang-framework-v1` (4) `BEHAVIORAL → experience-collaboration-v1` (5) `rubricCategory == EXPERIENCE → experience-technical-v1` (6) default `fallback-generic-v1`
6. 회귀 검증 — Rubric snapshot fixture diff 0 + `RubricLoaderTest` 모든 매핑 케이스 (InterviewType × QuestionType 조합) 기존 결과 동일

**P2 — 신규 흐름 + 분석 모델** (단일 PR):
1. QuestionType 정리 — RESUME_PLAYGROUND / RESUME_INTERROGATION 제거, RESUME_MAIN / RESUME_FOLLOWUP 추가. `isMain`/`isFollowUp`/`isResume` 분기 확장
2. AnswerAnalysis 재설계 — 신규 record (5 필드: claims / dimensionGaps / weakestDimension / unstatedAssumptions / recommendedNextAction) + `applyL1FalseNegativeGuard` 제거 + `turnId` / `mainQuestionId` / `withTurnId` / `withMainQuestionId` 필드·메서드 제거 + `TurnAnalysis` interface implements 제거 + `AnswerFeedbackPerspective` enum 폐기 + `AskedPerspectives` record 폐기. **추가: `TurnAnalysisResult` record + `TurnAnalysisPipeline` 서비스 폐기**. `AudioTurnAnalyzer.analyze` 반환 타입 = `AnswerAnalysis` 직접. 시그니처 변경 영향 = `FollowUpService:72,82,97` + `AudioTurnAnalyzer.java:45-66,92` + `TextFallbackTurnAnalyzer.java:30,46` (표준 트랙 회귀 = `FollowUpServiceTest` / `BehavioralFollowUpServiceTest` 통과 검증)
3. AudioTurnAnalyzerPromptBuilder / AnswerAnalyzerPromptBuilder / `AnswerAnalysisJsonRenderer` / `FollowUpPromptBuilder` / `PromptFormatters.formatPerspectives` / `GeneratedAnswerAnalysis.missingPerspectives` 필드 — prompt template 변경 `missing_perspectives` → `dimension_gaps + weakest_dimension`. perspective 인자 시그니처 제거
4. **응답/요청 DTO 정리** — `FollowUpResponse.selectedAnswerFeedbackPerspective` 필드 제거 + `FollowUpRequest.FollowUpExchange.selectedAnswerFeedbackPerspective` 필드 제거 + `GeneratedFollowUp.selectedAnswerFeedbackPerspective` 필드 제거 + LLM follow-up prompt 의 `selected_perspective` 응답 schema 제거. follow-up-experience.txt / follow-up-concept.txt template 정리
5. **`ResumeTrackInitiator` 책임 재작성 (재활용)** — 기존 클래스 유지, 의존 / 메서드 본문 재작성. Before = `ResumePlanPreparationService.prepare` + `runtimeStateStore.getOrInit` + `resumeInterviewOrchestrator.startSession` + `saveResults(emptyList)`. After = (a) `ResumeIngestionService.ingestExtractedText(resumeFileHash, resumeText)` 호출 (skeleton 추출 + persist 기존 유지) → (b) LLM 1회 호출 (`ResilientAiClient` callType=`resume_question_generator`, GPT-4o-mini primary + Claude fallback, opener N + main M (M=clamp(duration/3+2,7,40)) 일괄 생성) → (c) `transactionHandler.saveResults(interviewId, questionSets)` (RESUME_OPENER × N + RESUME_MAIN × M, orderIndex 순). 신규 클래스 (ResumeQuestionPlanner 등) 미도입
6. `FollowUpService.isResumeTrack` 분기 + `delegateToResumeOrchestrator` + 의존 (`ResumeInterviewOrchestrator`, `ResumeSkeletonRuntimeCache`, `InterviewPlanRuntimeCache`, `ResumeInterviewPlanner`) 제거
7. `StandardFollowUpPolicy.assertCanContinue` 분기 추가 — current main = RESUME_OPENER 이면 follow-up skip (응답 = aiSkip with "opener" reason). `RecommendedNextAction.SKIP` 분기는 표준 트랙 동일 패턴 유지
8. `FollowUpQuestionWriter` prompt — `target_claim_idx` 유지 + `weakestDimension` + ResumeSkeleton 동봉 hint. DialogueHistoryLayer 미참여
9. 폐기 (resume FSM) — `ResumeInterviewOrchestrator` / `PlaygroundModeHandler` / `InterrogationModeHandler` / `ChainStateTracker` / `ChainStateTrackerSnapshot` / `ChainReference` / `ChainStep` / `InterrogationChain` / `InterrogationPhase` / `PlaygroundPhase` / `Priority` / `ResumeClaim` / `ClaimType` / `StepType` / `ResumeMode` / `InterviewPlan` / `ProjectPlan` / `ProjectPlanListJsonConverter` / `ResumeInterviewPlanner` / `ResumeModeTransitionPolicy` / `ClockWatcher` / `InterviewPlanRuntimeCache` / `InterviewPlanPersister` / `ResumeReplanLoader` / `ResumeTurnEventPublisher` / `ResumeQuestionResultGenerator` / `ResumePlanPreparationService` / `PreparedResume` / `ResumeInterviewPlanValidator` / `ResumeTrackPolicy` / `InterviewTurnPolicyResolver`
10. 폐기 (컨텍스트) — `DialogueCompactor` / `DialogueHistoryLayer` / `SessionStateLayer` / `SessionStateSnapshot` (record) / `compaction_summarizer` callType / `SkeletonCallType.RESUME_PLAYGROUND_OPENER` / `SkeletonCallType.RESUME_PLAYGROUND_RESPONDER` / `FocusLayer.CAP_RESUME_PLAYGROUND_OPENER` / `FocusLayer.CAP_RESUME_PLAYGROUND_RESPONDER` / `FocusHints.ResumePlaygroundOpenerHints` / `FocusHints.ResumePlaygroundResponderHints` / `FocusHints.currentLevel` 필드 / `FocusHints.answerQuality` 필드 / `FocusLayer.CURRENT_LEVEL` prompt 라인 / `InterviewContextBuilder` layer 조합 갱신
11. **InterviewRuntimeState / InterviewRuntimeStateCache / ResumeSkeletonRuntimeCache 3 클래스 전면 폐기**. consumer 변경:
    - `FollowUpQuestionWriter` / `ResumeTrackInitiator` / `RubricScorerPromptBuilder` → `ResumeSkeletonPersister.findByInterviewId(id)` 직접 주입 (cache 미경유)
    - `AnswerAnalyzer.recordAnalysis(state, ...)` 호출 + `AudioTurnAnalyzer.recordAnalysis(state, ...)` 호출 제거
    - user level prompt hint = `Interview.userLevel` 직접 조회 (`FollowUpGenerationRequest.level`)
    - **`TurnAnalysis` interface 파일 폐기** + **`TurnAnalysisResult` record 폐기** + **`TurnAnalysisPipeline` 서비스 폐기** + `AnswerAnalysis.turnId` 필드 제거
12. **로그 마스킹 점검** — `infra/ai/logging/` LLM payload logger grep. 마스킹 정책 부재 시 추가 (INFO = `resumeId` + `call_type` + `fileHash` 만, payload = DEBUG). 현 정책 grep 결과 (`infra/ai/logging/` 디렉토리 존재 / 마스킹 헬퍼 부재 여부) implement 단 spec 박기
13. Service Integration / E2E / 결정적 회귀 / 로그 마스킹 테스트 추가
14. 운영 SQL 별도 스크립트 (`scripts/dev-cleanup-resume-legacy.sql`) — dev DB row truncate (P2 머지 직전 수동 실행)
15. AiCallMetrics — 신규 callType `resume_question_generator` 카운터 추가 + 폐기 `compaction_summarizer` dashboard panel 제거 메모 (`docs/observability/dashboards.md` — 파일 부재 시 신규 생성. P2 머지 시점 운영자 노트)

**P3 — 잔여 정리** (단일 PR):
1. `ResumeSkeleton.interrogationPriorityMap` 필드 + `priorityIds(...)` 메서드 제거. JSON 파싱 호환 검증 (`@JsonIgnoreProperties(ignoreUnknown = true)` 이미 적용 → 기존 record 데이터 무시). `ResumeSkeletonJsonCompatibilityTest` 추가
2. `interview_plan` 테이블 DROP (Flyway V48 — DDL 1개)
3. `ResumeArchitectureTest` (ArchUnit) 추가 — 폐기 클래스 부재 CI 가드 (§Appendix A 기준)
4. **`ResumeFallbackQuestions` / `ResumeFallbackBestAnswers` 폐기** — hardcoded fallback 질문 = 이력서 무관. LLM 실패 시 정책 변경 (initiator 실패 = 면접 시작 실패 / follow-up 실패 = next main 진행) 으로 fallback 클래스 불요
5. 잔여 grep 검증 — Pre/Post 정합 확인. line 수 측정 보고 (예상: domain/resume/ ~18 파일 / ~800줄)

## Appendix A — 폐기 클래스 / 메서드 / 필드 전체 목록 (grep 잔존 0 검증 기준)

§Verification "grep 잔존 0" + ArchUnit 단언 기준. P3 머지 시점 본 목록 전체 grep 0 확인.

### A-1. 클래스 / record / interface / enum

**Resume FSM**:
- `ResumeInterviewOrchestrator` / `PlaygroundModeHandler` / `InterrogationModeHandler` / `ResumeModeTransitionPolicy` / `ResumeReplanLoader` / `ResumeTurnEventPublisher` / `ResumeQuestionResultGenerator` / `ResumeTrackPolicy` / `InterviewTurnPolicyResolver` / `ClockWatcher`

**Chain / Plan 도메인**:
- `ChainStateTracker` / `ChainStateTrackerSnapshot` / `ChainReference` / `ChainStep` / `InterrogationChain` / `InterrogationPhase` / `PlaygroundPhase` / `Priority` / `ResumeClaim` / `ClaimType` / `StepType` / `ResumeMode` (enum) / `InterviewPlan` (entity) / `ProjectPlan` / `ProjectPlanListJsonConverter` / `ResumeInterviewPlanner` / `ResumeInterviewPlanValidator` / `ResumePlanPreparationService` / `PreparedResume` (record) / `InterviewPlanPersister` / `InterviewPlanRuntimeCache`

**Runtime / 캐시**:
- `InterviewRuntimeState` / `InterviewRuntimeStateCache` / `ResumeSkeletonRuntimeCache`

**분석 마커**:
- `TurnAnalysis` (interface) / `TurnAnalysisResult` (record) / `TurnAnalysisPipeline`

**Perspective 도메인**:
- `AnswerFeedbackPerspective` (enum) / `AskedPerspectives` (record)

**컨텍스트 / 컴팩션**:
- `DialogueCompactor` / `DialogueHistoryLayer` / `SessionStateLayer` / `SessionStateSnapshot` (record)

**Rubric 매핑 (YAML 잔재)**:
- `MappingResult` / `MappingRule` / `RubricResolutionContext` / `RubricMappingFamily`

**Fallback (P3 폐기 — 사용자 결정 확정)**:
- `ResumeFallbackQuestions` / `ResumeFallbackBestAnswers` — hardcoded fallback 질문 = 이력서 무관 → 면접 품질 저하. P3 단계에 폐기. LLM 실패 시 = follow-up 실패면 next main 진행, initiator bulk 실패면 면접 시작 실패

### A-2. 메서드 / 정적 팩토리

- `AnswerAnalysis.applyL1FalseNegativeGuard()` / `AnswerAnalysis.withTurnId(...)` / `AnswerAnalysis.withMainQuestionId(...)`
- `InterviewRuntimeState.recordAnalysis(...)` / `InterviewRuntimeState.getAnswerAnalysis(...)`
- `Rubric.selectDimensions(ResumeMode)` (`selectDimensions()` 무인자만 잔존)
- `RubricLoader.loadMapping()`
- `ResumeSkeleton.priorityIds(...)`
- `TurnCompletedEvent.ofResumeTrack(...)` / `TurnCompletedEvent.ofStandard(...)` (단일 `FollowUpQuestionCreatedEvent.of(...)` 만 잔존)
- `FollowUpTransactionHandler.publishTurnCompletedEvent(...)` (`publishFollowUpQuestionCreatedEvent` 로 재명명)

### A-3. 필드 / 상수

- `AnswerAnalysis.turnId` / `AnswerAnalysis.mainQuestionId` / `AnswerAnalysis.missingPerspectives` / `AnswerAnalysis.answerQuality`
- `FollowUpResponse.selectedAnswerFeedbackPerspective` / `FollowUpRequest.FollowUpExchange.selectedAnswerFeedbackPerspective` / `GeneratedFollowUp.selectedAnswerFeedbackPerspective`
- `GeneratedAnswerAnalysis.missingPerspectives`
- `FocusHints.currentLevel` / `FocusHints.answerQuality`
- `FocusHints.ResumePlaygroundOpenerHints` / `FocusHints.ResumePlaygroundResponderHints` (inner record)
- `InterviewRuntimeState.turnAnalysisCache` / `InterviewRuntimeState.coveredClaims` / `InterviewRuntimeState.coveredClaimsSet` / `InterviewRuntimeState.activeChain` / `InterviewRuntimeState.playgroundTurns` / `InterviewRuntimeState.compactedDialogueSummaries` / `InterviewRuntimeState.compactionInFlight` / `InterviewRuntimeState.resumeMode` / `InterviewRuntimeState.chainStateTracker` / `InterviewRuntimeState.interviewPlanCache` / `InterviewRuntimeState.resumeOrderCounter` / `InterviewRuntimeState.playgroundCumulativeLength` / `InterviewRuntimeState.currentLevel` / `InterviewRuntimeState.startedAt` / `InterviewRuntimeState.resumeSkeletonCache`
- `ResumeSkeleton.interrogationPriorityMap`
- `Rubric.perTurnRules` keys (`on_playground_mode` / `on_interrogation_mode`)
- `TurnCompletedEvent.turnIndex` / `TurnCompletedEvent.resumeMode` / `TurnCompletedEvent.currentChainLevel` / `TurnCompletedEvent.resumeSkeleton`
- `FocusLayer.CAP_RESUME_PLAYGROUND_OPENER` / `FocusLayer.CAP_RESUME_PLAYGROUND_RESPONDER`
- `SkeletonCallType.RESUME_PLAYGROUND_OPENER` / `SkeletonCallType.RESUME_PLAYGROUND_RESPONDER`

### A-4. enum 멤버 / 파일

- `QuestionType.RESUME_PLAYGROUND` / `QuestionType.RESUME_INTERROGATION` (enum 멤버)
- `resources/rubric/_mapping.yaml` (파일)

### A-5. prompt 토큰 / template 라인

- `FocusLayer.CURRENT_LEVEL` prompt 라인 (chain level 표시)
- LLM prompt 응답 schema `missing_perspectives` / `selected_perspective` 토큰
- prompt template `askedPerspectives` 인자 / `formatPerspectives` 호출
- callType `compaction_summarizer` (`AiCallMetrics` / `application-*.yml` / logger 사용처)

### A-6. ArchUnit 매핑

`ResumeArchitectureTest` (P3) 단언:
- `noClasses().that().haveSimpleName(...).should().exist()` × A-1 전체 목록
- `noMethods().that().haveName("applyL1FalseNegativeGuard").should().exist()`
- `noFields().that().haveName(...).should().exist()` × A-3 핵심 필드 (선택 — 비용 vs 가치 implement 단 결정)
