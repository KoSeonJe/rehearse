# Task 08 — FollowUpQuestionWriter prompt 갱신

> **위치**: `tasks/p2-be-08-followup-writer-prompt.md`
> **답하는 질문**: 꼬리질문 prompt 입력 어떻게 구성?

---

## 목적

`FollowUpQuestionWriter.write` prompt 입력 = 직전 main 질문 텍스트 + 사용자 답변 + `AnswerAnalysis` (`weakestDimension`/`dimensionGaps`/`claims`) + `ResumeSkeleton` 동봉. `DialogueHistoryLayer` / `SessionStateLayer` 미참여. `target_claim_idx` 유지 (표준 트랙 템플릿 호환).

## 에이전트

- **구현**: `backend` — FollowUpQuestionWriter prompt builder 입력 시그니처 + ResumeSkeleton 주입 + Layer 의존 제거
- **리뷰**: `code-reviewer-backend` — prompt 입력 정합 + DB 직접 주입 (cache 미경유)

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpQuestionWriter.java` — `write(...)` 인자 + 본문 갱신
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java` — `weakestDimension` + `ResumeSkeleton` 동봉 (이력서 트랙) + `target_claim_idx` 유지
- `backend/src/main/resources/ai/prompt/follow-up-experience.txt` — 이력서 트랙 prompt template (skeleton 영역 추가)
- `backend/src/main/resources/ai/prompt/follow-up-concept.txt` — 표준 트랙 (skeleton 미동봉) 분기 유지
- `backend/src/test/.../FollowUpQuestionWriterTest.java` — RESUME_MAIN 입력 시 skeleton 동봉 + `weakestDimension` 토큰 검증

## 핵심 로직

```java
public GeneratedFollowUp write(FollowUpContext ctx, AnswerAnalysis analysis) {
    Question currentMain = ctx.currentMainQuestion();

    // 이력서 트랙 = skeleton 동봉
    ResumeSkeleton skeleton = currentMain.getQuestionType().isResume()
        ? resumeSkeletonPersister.findByInterviewId(ctx.interviewId()).orElseThrow()
        : null;

    FollowUpPromptInput input = new FollowUpPromptInput(
        currentMain,
        ctx.userAnswer(),
        analysis.claims(),
        analysis.weakestDimension(),
        analysis.dimensionGaps(),
        skeleton,  // 이력서 트랙만 non-null
        ctx.previousExchanges()
    );

    String prompt = followUpPromptBuilder.build(input);
    GeneratedFollowUp generated;
    try {
        generated = resilientAiClient.call("follow_up_writer", prompt, GeneratedFollowUp.class);
    } catch (AiCallFailedException e) {
        log.warn("call_type=follow_up_writer 실패 → skip → next main", e);
        return GeneratedFollowUp.aiSkip("ai_failed");
    }
    return generated;
}
```

운영 로그: `log.info("follow-up 생성: weakestDimension={} target_claim_idx={}", ..., ...)` — tie / 분포 추적 (NF Observability).

## 의존
- 선행 Task: 02 (AnswerAnalysis `weakestDimension`/`dimensionGaps`/`claims`), 03 (prompt template), 04 (DTO 정리)
- 외부: `ResumeSkeletonPersister` (cache 미경유 — Task 11 정합)

## 테스트 케이스
- [ ] RESUME_MAIN 입력 시 prompt 에 `ResumeSkeleton.projects` JSON 동봉 + `weakest_dimension` 토큰 포함
- [ ] TECH_MAIN / BEHAVIORAL_MAIN 입력 시 skeleton 동봉 부재 (표준 트랙 회귀)
- [ ] `target_claim_idx` prompt 토큰 유지 (표준 트랙 follow-up 템플릿 호환)
- [ ] LLM primary + fallback 모두 실패 시 `GeneratedFollowUp.aiSkip("ai_failed")` 반환 (예외 전파 X) + WARN 로그
- [ ] 운영 로그 `weakestDimension=X target_claim_idx=Y` 노출 (logback capture)
- [ ] `DialogueHistoryLayer` / `SessionStateLayer` 의존 부재 (컴파일)

## 완료 기준
- [ ] prompt builder + writer 본문 + template 갱신
- [ ] 단위 + 통합 테스트 green
- [ ] grep `DialogueHistoryLayer` / `SessionStateLayer` FollowUpQuestionWriter 내 0
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): FollowUpQuestionWriter prompt weakestDimension + skeleton 동봉
```

## 비고

`ResumeSkeletonPersister.findByInterviewId` 직접 호출 = `ResumeSkeletonRuntimeCache` 미경유. LLM ~300ms 대비 DB ~5ms ROI 미미 (tech-spec §Data Model 7).
