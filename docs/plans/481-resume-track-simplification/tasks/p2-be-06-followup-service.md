# Task 06 — FollowUpService delegateToResumeOrchestrator 분기 제거

> **위치**: `tasks/p2-be-06-followup-service.md`
> **답하는 질문**: FollowUpService 에서 Resume 트랙 분기 어떻게 제거?

---

## 목적

`FollowUpService.generateFollowUp()` line 64-79 의 `delegateToResumeOrchestrator` 분기 + Resume 전용 의존 (`ResumeInterviewOrchestrator`, `ResumeSkeletonRuntimeCache`, `InterviewPlanRuntimeCache`, `ResumeInterviewPlanner`) 제거. 이력서 트랙도 표준 트랙 코드 경로 사용.

## 에이전트

- **구현**: `backend` — FollowUpService 분기 제거 + 의존 정리 + AudioTurnAnalyzer 신규 시그니처 적응
- **리뷰**: `code-reviewer-backend` — 표준 트랙 회귀 / `recommendedNextAction.SKIP` 분기 유지

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — `isResumeTrack` 분기 + `delegateToResumeOrchestrator` + Resume 전용 의존 4개 제거. `AudioTurnAnalyzer.analyze` 반환 = `AnswerAnalysis` 직접 (line 72,82,97 갱신)
- `backend/src/test/.../FollowUpServiceTest.java` — CS 트랙 회귀
- `backend/src/test/.../BehavioralFollowUpServiceTest.java` — 회귀
- `backend/src/test/.../FollowUpServiceResumeFlowTest.java` — 신규 (RESUME_OPENER → follow-up 미생성 / RESUME_MAIN → RESUME_FOLLOWUP 1개 생성)

## 핵심 로직

```java
@Transactional
public FollowUpResponse generateFollowUp(Long interviewId, FollowUpRequest request) {
    FollowUpContext ctx = followUpContextLoader.load(interviewId, request);

    AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
        ctx.currentMainQuestion(),
        ctx.userAnswer(),
        ctx.previousExchanges()
    );

    if (analysis.recommendedNextAction() == RecommendedNextAction.SKIP) {
        return handleAnalyzerSkip(ctx, analysis);
    }

    standardFollowUpPolicy.assertCanContinue(ctx.currentMainQuestion());  // Task 07 분기 추가

    GeneratedFollowUp generated = followUpQuestionWriter.write(ctx, analysis);
    return followUpTransactionHandler.saveFollowUpResultAndPublishEvent(ctx, analysis, generated);
}

// 제거:
// - private boolean isResumeTrack(...)
// - private FollowUpResponse delegateToResumeOrchestrator(...)
// - ResumeInterviewOrchestrator / ResumeSkeletonRuntimeCache / InterviewPlanRuntimeCache / ResumeInterviewPlanner 의존
```

## 의존
- 선행 Task: 02 (AudioTurnAnalyzer 시그니처), 05 (ResumeTrackInitiator 재작성 — 면접 시작 단 책임 분리)
- 외부: 없음

## 테스트 케이스
- [ ] `FollowUpServiceTest` (CS / TECH) — 기존 케이스 모두 통과 (회귀)
- [ ] `BehavioralFollowUpServiceTest` — 회귀
- [ ] `FollowUpServiceResumeFlowTest` — RESUME_OPENER main 응답 시 `aiSkip=true` + RESUME_FOLLOWUP row 미생성
- [ ] `FollowUpServiceResumeFlowTest` — RESUME_MAIN main 응답 시 RESUME_FOLLOWUP row 1개 생성 (orderIndex 순)
- [ ] `recommendedNextAction.SKIP` 분기 = `handleAnalyzerSkip` 호출 (표준 트랙 동일)
- [ ] grep `delegateToResumeOrchestrator` / `isResumeTrack` / `ResumeInterviewOrchestrator` 의존 0 (FollowUpService 내)

## 완료 기준
- [ ] 분기 + 의존 제거 + 통합 테스트 green
- [ ] 표준 트랙 회귀 0
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): FollowUpService Resume 분기 제거 - 표준 트랙 코드 경로 단일화
```

## 비고

`recommendedNextAction.SKIP` 분기 = 표준 트랙 안전망 (답변 이탈 / 시간 만료 임박). product-spec Goal "꼬리질문 결정 코드 분기 0" 의 의도 (Chain FSM 깊이 강제 폐기) 와 별개 책임으로 유지.
