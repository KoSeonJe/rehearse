# Task 11 — InterviewRuntimeState 3 클래스 전면 폐기

> **위치**: `tasks/p2-be-11-runtime-state-discard.md`
> **답하는 질문**: RuntimeState 캐시 어떻게 제거하고 consumer 어떻게 전환?

---

## 목적

`InterviewRuntimeState` (13 필드 모두 dead read) + `InterviewRuntimeStateCache` + `ResumeSkeletonRuntimeCache` 3 클래스 전면 폐기. consumer = `ResumeSkeletonPersister` 직접 주입 (cache 미경유). user level prompt hint = `Interview.userLevel` 직접 조회. backend stateless = 표준 트랙 동일 패턴.

## 에이전트

- **구현**: `backend` — 3 클래스 파일 삭제 + consumer 4곳 직접 주입 전환 + recordAnalysis 호출 제거
- **리뷰**: `code-reviewer-backend` — 동시성 위험 / 시그니처 회귀 / DB 호출 빈도

## 변경 파일

**삭제 (3 클래스)**:
- `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewRuntimeState.java`
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewRuntimeStateCache.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonRuntimeCache.java`

**Consumer 전환 (직접 주입)**:
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpQuestionWriter.java` — Task 08 정합 (이미 `ResumeSkeletonPersister.findByInterviewId` 직접)
- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java` — Task 05 정합 (이미 `ResumeIngestionService` 직접)
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorerPromptBuilder.java` — `ResumeSkeletonPersister.findByInterviewId(interviewId)` 직접 주입 (현재 `RubricScorer.score` 시그니처로 전달받음 → 자체 주입 전환)
- `backend/src/main/java/com/rehearse/api/infra/ai/analyzer/AnswerAnalyzer.java` — `recordAnalysis(state, ...)` 호출 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/analyzer/AudioTurnAnalyzer.java` — `recordAnalysis(state, ...)` 호출 제거 (Task 02 정합)

**Level Hint 전환**:
- `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpGenerationRequest.java` — `level` 필드 = `Interview.userLevel` 직접 조회 (FollowUpContextLoader 가 주입)
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpContextLoader.java` — `interview.userLevel` 매핑

**테스트 정리**:
- `backend/src/test/...` — `InterviewRuntimeState*` / `ResumeSkeletonRuntimeCache*` 단위 테스트 삭제
- 통합 테스트 = `FollowUpServiceTest` / `ResumeTrackInitiatorIntegrationTest` 회귀 통과

## 핵심 로직

```java
// RubricScorerPromptBuilder (After)
public class RubricScorerPromptBuilder {
    private final ResumeSkeletonPersister resumeSkeletonPersister;  // 신규 의존

    public String build(Question q, QuestionSet qs, Interview interview, ...) {
        ResumeSkeleton skeleton = q.getQuestionType().isResume()
            ? resumeSkeletonPersister.findByInterviewId(interview.getId()).orElse(null)
            : null;
        // ... 기존 prompt 구성. skeleton 인자 전달 부재
    }
}

// AnswerAnalyzer (After) — recordAnalysis 호출 제거
public AnswerAnalysis analyze(...) {
    GeneratedAnswerAnalysis generated = ...;
    return mapToAnswerAnalysis(generated);
    // 제거: runtimeState.recordAnalysis(...)
}
```

## 의존
- 선행 Task: 02 (AudioTurnAnalyzer 시그니처), 05 (ResumeTrackInitiator), 08 (FollowUpQuestionWriter), 09 (Resume FSM 폐기 — Plan 의존 정리 선행), 10 (Context Layer — SessionStateLayer 폐기 선행)
- 외부: 없음

## 테스트 케이스
- [ ] `./gradlew compileJava` 통과 (`InterviewRuntimeState` / 캐시 의존 0)
- [ ] `FollowUpServiceTest` (CS / TECH) 회귀 통과
- [ ] `ResumeTrackInitiatorIntegrationTest` (Task 13) 통과
- [ ] `RubricScorerPromptBuilder` 회귀 = 채점 결과 동일 (Phase 1 snapshot diff 유지)
- [ ] grep `InterviewRuntimeState` / `InterviewRuntimeStateCache` / `ResumeSkeletonRuntimeCache` / `recordAnalysis` / `getAnswerAnalysis` / `turnAnalysisCache` 잔존 0
- [ ] DB 호출 빈도 — follow-up 1회당 `ResumeSkeletonPersister.findByInterviewId` 호출 1~2회 (writer + rubric scorer 각 1) ≈ ~5ms × 2 = ~10ms 추가. LLM ~300ms 대비 ROI 미미

## 완료 기준
- [ ] 3 클래스 파일 삭제 + consumer 4곳 직접 주입 전환
- [ ] 표준 + Resume 트랙 회귀 통과
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): InterviewRuntimeState + Skeleton 캐시 전면 폐기 - Persister 직접 주입
```

## 비고

- 캐시 폐기 ROI = LLM ~300ms 대비 DB ~5ms 미미 (tech-spec §Data Model 7)
- 동시성 위험 0 = backend stateless 패턴 = 표준 트랙 동일
- Skeleton 캐시 추가 필요 시 future 작업 (본 plan 비스코프)
