# Plan 01: OpenAI GPT-4o-mini Client 구현

> 상태: Draft
> 작성일: 2026-03-26

## Why

Claude API의 rate limit 문제를 해결하기 위해 Primary AI를 GPT-4o-mini로 전환한다. OpenAI Chat Completions API를 호출하는 새 클라이언트를 구현하되, 기존 프롬프트 빌더와 응답 파서를 최대한 재사용한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/.../infra/ai/dto/OpenAiRequest.java` | OpenAI Chat Completions 요청 DTO 생성 |
| `backend/src/main/java/.../infra/ai/dto/OpenAiResponse.java` | OpenAI Chat Completions 응답 DTO 생성 |
| `backend/src/main/java/.../infra/ai/OpenAiClient.java` | GPT-4o-mini 호출 클라이언트 생성 |
| `backend/src/main/java/.../infra/ai/ClaudeResponseParser.java` | `AiResponseParser`로 rename (로직 변경 없음) |
| `backend/src/main/java/.../infra/ai/ClaudeApiClient.java` | AiResponseParser 참조 변경 |

## 상세

### OpenAiRequest DTO

OpenAI Chat Completions API 형식:
```java
@Builder
public record OpenAiRequest(
    String model,
    List<Message> messages,
    @JsonProperty("max_tokens") int maxTokens,
    Double temperature
) {
    @Builder
    public record Message(String role, String content) {}
}
```

### OpenAiResponse DTO

```java
public record OpenAiResponse(
    String id,
    String model,
    List<Choice> choices,
    Usage usage
) {
    public record Choice(int index, Message message, @JsonProperty("finish_reason") String finishReason) {}
    public record Message(String role, String content) {}
    public record Usage(@JsonProperty("prompt_tokens") int promptTokens,
                       @JsonProperty("completion_tokens") int completionTokens,
                       @JsonProperty("total_tokens") int totalTokens) {}
}
```

### OpenAiClient 구현

- `@Component`, `@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")`
- **AiClient 인터페이스 미구현** (ResilientAiClient가 AiClient 구현)
- RestClient 기반 HTTP 호출: `https://api.openai.com/v1/chat/completions`
- `Authorization: Bearer {api-key}` 헤더
- 모델: `gpt-4o-mini` (질문 생성 + 후속 질문 동일 모델)
- max_tokens: 질문 8192 / 후속 1024
- temperature: 질문 0.9 / 후속 1.0
- 프롬프트 빌더 재사용: system prompt → `role: "system"`, user prompt → `role: "user"`
- **재시도 1회** (총 2회 시도): 수동 for-loop, 지수 백오프 1초. 기존 ClaudeApiClient의 3회 재시도를 1회로 축소
- HTTP 에러 처리: 429 → RetryableApiException, 4xx → BusinessException, 5xx → RetryableApiException
- 2회 모두 실패 시 최종 예외 throw → ResilientAiClient에서 Claude fallback 처리

### AiResponseParser (rename)

- `ClaudeResponseParser` → `AiResponseParser` 순수 rename
- 마크다운 코드블록 JSON 추출 로직은 모델 무관 → 변경 없음
- 로그 메시지 "Claude" → "AI"

### ClaudeApiClient 수정

- `implements AiClient` 제거 → 독립 클래스
- `ClaudeResponseParser` → `AiResponseParser` 참조 변경
- `@RateLimiter(name = "claude-api")` 유지 (fallback 시에도 rate limit 필요)
- 나머지 로직 변경 없음 (재시도, 프롬프트 빌더 등 그대로)

## 담당 에이전트

- Implement: `backend` — DTO + Client + Parser rename
- Review: `architect-reviewer` — 구조 일관성, SOLID, 레이어링

## 검증

- `./gradlew build` 성공 (컴파일 에러 없음)
- ClaudeApiClient 기존 동작 유지 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
