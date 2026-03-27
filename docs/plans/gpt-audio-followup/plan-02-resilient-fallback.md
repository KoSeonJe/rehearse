# Plan 02: ResilientAiClient fallback 경로

> 상태: Draft
> 작성일: 2026-03-27

## Why

GPT-4o-mini-audio-preview가 실패할 경우(API 장애, 429, 타임아웃 등), 기존 Whisper STT + Claude 텍스트 생성으로 fallback해야 서비스 안정성을 보장할 수 있다. 기존 `ResilientAiClient`의 primary/fallback 패턴을 확장한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` | `generateFollowUpWithAudio()` 구현 (primary + fallback) |

## 상세

### ResilientAiClient.java

```java
@Override
public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
    // Primary: GPT-4o-mini-audio
    if (openAiClient != null) {
        try {
            return openAiClient.generateFollowUpWithAudio(audioFile, request);
        } catch (BusinessException e) {
            if (isNonRetryableError(e)) throw e;  // CLIENT_ERROR, PARSE_FAILED는 fallback 불가
            log.warn("[AI Fallback] GPT-audio 실패 → Whisper+Claude 전환: {}", e.getMessage());
        }
    }

    // Fallback: Whisper STT + Claude 텍스트 생성
    return fallbackWithSttAndClaude(audioFile, request);
}

private GeneratedFollowUp fallbackWithSttAndClaude(MultipartFile audioFile, FollowUpGenerationRequest request) {
    // 1. Whisper STT로 transcript 추출
    String answerText = sttService.transcribe(audioFile);

    // 2. FollowUpGenerationRequest는 record이므로 new로 재생성
    FollowUpGenerationRequest updatedReq = new FollowUpGenerationRequest(
        request.position(), request.techStack(), request.level(),
        request.questionContent(), answerText,
        request.nonVerbalSummary(), request.previousExchanges()
    );
    GeneratedFollowUp followUp = claudeApiClient.generateFollowUpQuestion(updatedReq);

    // 3. answerText를 결과에 포함
    return followUp.withAnswerText(answerText);
}
```

핵심 포인트:
- `SttService` 의존성 추가 (기존에는 ResilientAiClient에 없었음)
- fallback 경로: Whisper → Claude (기존 `generateFollowUpQuestion` 재사용)
- `isNonRetryableError()` 판단: CLIENT_ERROR, PARSE_FAILED는 Claude로 보내도 동일 실패이므로 fallback 안 함
- `FollowUpGenerationRequest`는 record이므로 `withAnswerText()` 대신 new 생성자 사용

## 담당 에이전트

- Implement: `backend` — fallback 로직 구현
- Review: `code-reviewer` — 에러 핸들링, 재시도 로직 검증

## 검증

- GPT-audio 정상 시 primary 경로 동작 확인
- GPT-audio 실패(mock) 시 Whisper + Claude fallback 동작 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
