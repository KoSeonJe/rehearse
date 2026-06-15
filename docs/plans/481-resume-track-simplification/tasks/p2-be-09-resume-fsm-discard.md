# Task 09 — Resume FSM 25+ 클래스 폐기

> **위치**: `tasks/p2-be-09-resume-fsm-discard.md`
> **답하는 질문**: 이력서 FSM 도메인 어떻게 제거?

---

## 목적

이력서 트랙 Mode FSM + Chain FSM + InterviewPlan 사전 계획 + 모드 핸들러 전면 폐기. Task 05 (ResumeTrackInitiator 재작성) + Task 06 (FollowUpService 분기 제거) 완료 후 호출자 0 확인 → 클래스 파일 삭제.

## 에이전트

- **구현**: `backend` — domain/resume/ + domain/interview/ + 의존 클래스 파일 일괄 삭제 + 잔존 참조 정리
- **리뷰**: `code-reviewer-backend` — 컴파일 + grep 잔존 + 회귀

## 변경 파일 (삭제 25+)

**Resume FSM**:
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java` (256줄)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java` (158줄)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` (147줄)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeModeTransitionPolicy.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ClockWatcher.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeReplanLoader.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java`

**Chain / Plan 도메인**:
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStateTracker.java` (126줄)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ChainStateTrackerSnapshot.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainReference.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ChainStep.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/InterrogationChain.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/InterrogationPhase.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/PlaygroundPhase.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/Priority.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeClaim.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ClaimType.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/StepType.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeMode.java` (enum)
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/InterviewPlan.java` (entity)
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ProjectPlan.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ProjectPlanListJsonConverter.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewPlanner.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewPlanValidator.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumePlanPreparationService.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/PreparedResume.java` (record)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanPersister.java`
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterviewPlanRuntimeCache.java`

**테스트 삭제**:
- `backend/src/test/...` — 위 클래스 단위 / 통합 테스트 일괄 삭제

## 핵심 로직

```bash
# 1. 클래스 파일 일괄 삭제
# 2. 컴파일 확인 — 잔존 참조 발견 시 정리
# 3. 통합 테스트 회귀
# 4. grep 잔존 0 확인
```

## 의존
- 선행 Task: 05 (ResumeTrackInitiator 재작성 — `ResumePlanPreparationService` / `ResumeInterviewOrchestrator` 호출자 0 확정), 06 (FollowUpService 분기 제거 — Resume 의존 정리)
- 외부: 없음

## 테스트 케이스
- [ ] `./gradlew compileJava compileTestJava` 통과 (잔존 참조 0)
- [ ] `./gradlew test` 표준 트랙 + RESUME 신규 흐름 회귀 통과
- [ ] grep tech-spec §Appendix A-1 "Resume FSM" / "Chain / Plan 도메인" 항목 모두 잔존 0

## 완료 기준
- [ ] 25+ 파일 일괄 삭제
- [ ] 컴파일 + 테스트 통과
- [ ] grep 잔존 0 (tech-spec §Appendix A-1)
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): Resume FSM 도메인 25+ 클래스 폐기
```

## 비고

`interview_plan` 테이블 DROP 은 Task P3 (V48 Flyway). 본 task = 엔티티 / 서비스 코드만 삭제.
