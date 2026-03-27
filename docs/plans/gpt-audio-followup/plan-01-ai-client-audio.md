# Plan 01: AiClient 인터페이스 + OpenAiClient audio 메서드

> 상태: Draft
> 작성일: 2026-03-27

## Why

현재 `AiClient` 인터페이스는 텍스트 기반 `generateFollowUpQuestion()`만 제공한다. GPT-4o-mini-audio는 오디오를 직접 입력받아 STT + 후속질문 생성을 한 번에 처리하므로, 오디오 입력을 받는 새 메서드가 필요하다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java` | `generateFollowUpWithAudio()` 메서드 추가 |
| `backend/src/main/java/com/rehearse/api/infra/ai/OpenAiClient.java` | GPT-4o-mini-audio-preview 호출 구현 |
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/OpenAiRequest.java` | audio content part 지원 (필요 시) |

## 상세

### AiClient.java

```java
GeneratedFollowUp generateFollowUpWithAudio(
    MultipartFile audioFile,
    FollowUpGenerationRequest request
);
```

- 기존 `generateFollowUpQuestion(FollowUpGenerationRequest)` 유지 (fallback + 질문 생성에서 사용)
- 새 메서드는 오디오 파일을 받아 transcript + 후속질문을 한 번에 반환

### OpenAiClient.java

```java
@RateLimiter(name = "openai-api")
public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
    // 1. audioFile → base64 인코딩
    // 2. model: "gpt-4o-mini-audio-preview"
    // 3. messages: [
    //      {role: "system", content: followUpPromptBuilder.buildSystemPrompt(request)},
    //      {role: "user", content: [
    //          {type: "text", text: followUpPromptBuilder.buildUserPromptForAudio(request)},
    //          {type: "input_audio", input_audio: {data: base64, format: "webm"}}
    //      ]}
    //    ]
    // 4. 응답 파싱: answerText + followUpQuestion + reason + type
}
```

핵심 포인트:
- `RestClient`로 OpenAI Chat Completions API 호출 (기존 패턴 재사용)
- 오디오는 base64 인코딩하여 `input_audio` 타입으로 전달
- 프롬프트에 "먼저 오디오를 전사하고, 그 내용을 바탕으로 후속질문을 생성하라" 지시
- 응답 JSON에 `answerText` 필드 포함하도록 프롬프트 설계
- 기존 `callOpenAiApi()` 재시도 로직 재사용

## 담당 에이전트

- Implement: `backend` — API 호출 구현
- Review: `architect-reviewer` — 인터페이스 설계, 기존 패턴과의 일관성

## 검증

- `OpenAiClient.generateFollowUpWithAudio()` 단위 테스트 (Mock 응답)
- 실제 GPT-4o-mini-audio-preview 호출 확인 (수동 테스트)
- `progress.md` 상태 업데이트 (Task 1 → Completed)
