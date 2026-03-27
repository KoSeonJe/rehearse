# Plan 04: DTO 수정 + 코드 정리

> 상태: Draft
> 작성일: 2026-03-27

## Why

GPT-audio가 transcript(answerText)도 함께 반환하므로, 기존 `GeneratedFollowUp` DTO에 `answerText` 필드를 추가해야 한다. 또한 `MockAiClient`에도 새 메서드 구현이 필요하다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedFollowUp.java` | `answerText` 필드 + `withAnswerText()` 추가 |
| `backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java` | `generateFollowUpWithAudio()` mock 구현 |
| `backend/src/main/java/com/rehearse/api/infra/ai/ClaudeApiClient.java` | 변경 없음 (AiClient 미구현 구조) |

## 상세

### GeneratedFollowUp.java

```java
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedFollowUp {
    private String question;
    private String reason;
    private String type;
    private String modelAnswer;
    private String answerText;  // 추가: GPT-audio가 추출한 transcript

    public GeneratedFollowUp withAnswerText(String answerText) {
        this.answerText = answerText;
        return this;
    }
}
```

`@NoArgsConstructor` + Jackson 역직렬화 패턴 유지 (기존 DTO와 일관). `withAnswerText()`는 fallback 경로에서 Whisper 결과를 주입할 때 사용.

### MockAiClient.java

```java
@Override
public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
    log.info("[Mock] generateFollowUpWithAudio 호출");
    return GeneratedFollowUp.builder()
        .question("Mock 후속 질문입니다.")
        .reason("Mock 이유입니다.")
        .type("DEEP_DIVE")
        .answerText("Mock 답변 텍스트입니다.")
        .build();
}
```

### ClaudeApiClient.java — 변경 없음

`ClaudeApiClient`는 `AiClient` 인터페이스를 구현하지 않고, `ResilientAiClient`가 직접 참조하는 구조다. `generateFollowUpWithAudio()` 메서드를 추가할 필요가 없다. fallback 경로에서는 `ResilientAiClient`가 Whisper STT + `claudeApiClient.generateFollowUpQuestion()`을 직접 조합한다.

## 담당 에이전트

- Implement: `backend` — DTO 수정, mock 구현
- Review: `code-reviewer` — DTO 일관성, mock 동작 확인

## 검증

- `GeneratedFollowUp`에 `answerText` 필드 존재 확인
- Mock 모드에서 `generateFollowUpWithAudio()` 정상 반환 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
