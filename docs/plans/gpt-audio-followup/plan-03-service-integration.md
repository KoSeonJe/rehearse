# Plan 03: InterviewService 통합

> 상태: Draft
> 작성일: 2026-03-27

## Why

현재 `InterviewService.generateFollowUp()`은 Phase 1에서 `resolveAnswerText()`로 Whisper STT를 별도 호출한 뒤, Phase 3에서 `aiClient.generateFollowUpQuestion()`으로 후속질문을 생성한다. GPT-audio 통합 후에는 `aiClient.generateFollowUpWithAudio()`가 두 단계를 한 번에 처리하므로, 서비스 레이어를 단순화한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java` | `generateFollowUp()` 수정, `resolveAnswerText()` 삭제 |

## 상세

### 변경 전 (현재)

```java
public FollowUpResponse generateFollowUp(Long id, FollowUpRequest request, MultipartFile audioFile) {
    // Phase 1: STT (트랜잭션 없음)
    String answerText = resolveAnswerText(request, audioFile);

    // Phase 2: DB 조회 (readOnly TX)
    FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, request.getQuestionSetId());

    // Phase 3: AI API 호출 (트랜잭션 없음)
    GeneratedFollowUp followUp = aiClient.generateFollowUpQuestion(followUpReq);

    // Phase 4: 저장 (write TX)
    Question savedQuestion = followUpTransactionHandler.saveFollowUpResult(...);
}
```

### 변경 후

```java
public FollowUpResponse generateFollowUp(Long id, FollowUpRequest request, MultipartFile audioFile) {
    // Phase 1: DB 조회 (readOnly TX) — 먼저 컨텍스트 로드
    FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, request.getQuestionSetId());

    // Phase 2: GPT-audio 호출 (트랜잭션 없음) — STT + 후속질문 한 번에
    FollowUpGenerationRequest followUpReq = new FollowUpGenerationRequest(
        context.position(), context.effectiveTechStack(), context.level(),
        request.getQuestionContent(), null, // answerText는 GPT-audio가 추출
        request.getNonVerbalSummary(), request.getPreviousExchanges()
    );
    GeneratedFollowUp followUp = aiClient.generateFollowUpWithAudio(audioFile, followUpReq);

    // Phase 3: 저장 (write TX)
    Question savedQuestion = followUpTransactionHandler.saveFollowUpResult(
        context.questionSetId(), followUp, context.nextOrderIndex());

    return FollowUpResponse.builder()
        .questionId(savedQuestion.getId())
        .question(followUp.getQuestion())
        .reason(followUp.getReason())
        .type(followUp.getType())
        .answerText(followUp.getAnswerText())  // GPT-audio가 추출한 transcript
        .modelAnswer(savedQuestion.getModelAnswer())
        .build();
}
```

핵심 변경:
- `resolveAnswerText()` 메서드 삭제
- Phase 순서 변경: DB 조회 → GPT-audio → 저장 (3단계로 단순화)
- `SttService` 직접 의존 제거 (ResilientAiClient fallback에서만 사용)
- TX 분리 패턴(`@Transactional(propagation = NOT_SUPPORTED)`) 유지

## 담당 에이전트

- Implement: `backend` — 서비스 로직 수정
- Review: `architect-reviewer` — TX 분리 패턴 유지 확인, Phase 구조 검증

## 검증

- `generateFollowUp()` 정상 동작 확인 (answerText 포함 응답)
- TX 분리 패턴 유지 확인 (외부 API 호출 시 커넥션 미점유)
- `progress.md` 상태 업데이트 (Task 3 → Completed)
