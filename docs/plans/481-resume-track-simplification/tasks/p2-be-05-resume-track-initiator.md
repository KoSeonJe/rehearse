# Task 05 — ResumeTrackInitiator 책임 재작성

> **위치**: `tasks/p2-be-05-resume-track-initiator.md`
> **답하는 질문**: 면접 시작 시 opener + main 어떻게 일괄 생성?

---

## 목적

기존 클래스 유지 + 책임 전면 재작성. `ResumePlanPreparationService.prepare` + `runtimeStateStore.getOrInit` + `resumeInterviewOrchestrator.startSession` + `saveResults(emptyList)` → skeleton ingest + LLM 1회 (opener N + main M 일괄) + saveResults. 신규 클래스 도입 0 (`simplicity.md` 준수).

## 에이전트

- **구현**: `backend` — ResumeTrackInitiator 의존 / 메서드 본문 재작성 + LLM prompt + duration clamp + 실패 정책
- **리뷰**: `code-reviewer-backend` — LLM 호출 / 토큰 한계 / 실패 처리 / 트랜잭션 경계

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java` — 의존 + 메서드 본문 전면 재작성
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionGenerationService.java` — RESUME_BASED 분기에서 `ResumeTrackInitiator.initiate` 호출 (시그니처 정합 확인)
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionGenerationTransactionHandler.java` — `saveResults` 호출자 — RESUME_OPENER × N + RESUME_MAIN × M, orderIndex 순 저장 확인
- `backend/src/main/resources/ai/prompt/resume-question-generator.txt` — 신규 prompt template (opener N + main M 일괄 생성)
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeQuestionGeneratorPromptBuilder.java` — 신규 (또는 기존 prompt builder 재활용)
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedResumeQuestions.java` — 신규 DTO (`{"openers":[...], "mains":[...]}`)
- `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` 또는 callType 매핑 — `resume_question_generator` 신규 callType (GPT-4o-mini primary + Claude fallback)
- `backend/src/main/resources/application-*.yml` — callType 모델 매핑 추가
- `backend/src/test/.../ResumeTrackInitiatorIntegrationTest.java` — Service Integration (mock LLM fixture)
- `backend/src/test/resources/fixtures/resume-question-generator-response.json` — 신규 fixture

## 핵심 로직

```java
@Transactional
public void initiate(Long interviewId, String resumeFileHash, String resumeText) {
    // 1. skeleton ingest (기존 유지)
    ResumeSkeleton skeleton = resumeIngestionService.ingestExtractedText(resumeFileHash, resumeText);

    // 2. duration clamp + mainCount
    Interview interview = interviewRepository.findById(interviewId).orElseThrow();
    int durationMinutes = Math.max(15, Math.min(120, interview.getDurationMinutes()));
    int mainCount = Math.min(40, durationMinutes / 3 + 2);

    // 3. LLM 1회 호출 (callType=resume_question_generator)
    GeneratedResumeQuestions generated;
    try {
        generated = resilientAiClient.call(
            "resume_question_generator",
            promptBuilder.build(skeleton, mainCount),
            GeneratedResumeQuestions.class
        );
    } catch (AiCallFailedException e) {
        // primary + fallback 모두 실패 = 면접 시작 실패
        interviewService.markFailed(interviewId, "LLM 호출 실패");
        throw new BusinessException(INTERVIEW_START_FAILED);
    }

    // 4. QuestionSet 적재 (RESUME_OPENER × N + RESUME_MAIN × M, orderIndex 순)
    List<Question> questions = mapToQuestions(generated, interviewId);  // orderIndex 순서 보장
    transactionHandler.saveResults(interviewId, questions);
}
```

prompt 가이드:
- "스켈레톤 내 `projects` / 기술 topic 만 활용"
- "opener N=2~3개 = 프로젝트 설명 요청 (꼬리질문 없음). 답변 길이 / 깊이 강제 X"
- "main M개 = 기술 topic 깊이 질문. 각 main 마다 best answer 동봉"
- per-main 토큰 보수 제한: 질문 50 + topic 20 + bestAnswer 300 ≈ 370 tokens

## 의존
- 선행 Task: 01 (QuestionType — RESUME_OPENER / RESUME_MAIN 사용)
- 외부: `ResilientAiClient` + `ResumeIngestionService` + `ResumeSkeletonPersister`

## 테스트 케이스
- [ ] `ResumeTrackInitiatorIntegrationTest` — durationMinutes=30 입력 시 mainCount=12 일괄 생성 (clamp 미적용)
- [ ] durationMinutes=10 입력 시 clamp 15 적용 → mainCount=7
- [ ] durationMinutes=200 입력 시 clamp 120 적용 → mainCount=40
- [ ] LLM mock fixture (`resume-question-generator-response.json`) 응답 시 RESUME_OPENER × N + RESUME_MAIN × M, orderIndex 순서 저장
- [ ] LLM primary + fallback 모두 실패 시 `BusinessException(INTERVIEW_START_FAILED)` throw + `InterviewStatus.FAILED` 전환
- [ ] 부분 응답 (truncate `finish_reason=length`) = 실패 처리 (적재 금지)
- [ ] 생성된 RESUME_OPENER 모두 `RubricCategory.EXPERIENCE`, RESUME_MAIN 모두 `TECHNICAL`
- [ ] 생성된 topic 모두 `skeleton.projects` 범위 (prompt 가이드 검증 — fixture 기반)

## 완료 기준
- [ ] 메서드 본문 재작성 + 통합 테스트 green
- [ ] 신규 callType `resume_question_generator` GPT-4o-mini primary + Claude fallback 동작
- [ ] grep `ResumePlanPreparationService` / `PreparedResume` / `resumeInterviewOrchestrator.startSession` ResumeTrackInitiator 내부 호출 0
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): ResumeTrackInitiator 책임 재작성 - skeleton ingest + LLM 1회 일괄 생성
```

## 비고

- R6 위험 (응답 토큰 한계) — M max 40 clamp + per-main 보수 제한으로 ~14.8K (16.4K 한계 대비 ~1.6K 안전 마진)
- chunk 분할 회피 사유 = chunk 경계 topic 중복 = Goal 위배
- 운영 SQL `dev-cleanup-resume-legacy.sql` (Task 14) 머지 직전 수동 실행 필요
- `ResumePlanPreparationService` / `PreparedResume` 등 폐기 클래스 = Task 09 진행
