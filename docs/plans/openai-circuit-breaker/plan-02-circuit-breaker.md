# Plan 02: 서킷브레이커 + Fallback 구조

> 상태: Draft
> 작성일: 2026-03-26

## Why

외부 API(OpenAI) 장애 시 서비스 전체가 마비되는 SPOF를 제거한다. 서킷브레이커로 장애를 감지하고, OPEN 시 Claude API로 자동 전환하여 서비스 연속성을 보장한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/.../infra/ai/ResilientAiClient.java` | Composite AiClient 생성 (CB + fallback) |
| `backend/src/main/java/.../infra/ai/ClaudeApiClient.java` | `implements AiClient` 제거, 독립 클래스화 |
| `backend/src/main/java/.../infra/ai/MockAiClient.java` | 조건 변경 → `@ConditionalOnMissingBean(ResilientAiClient.class)` |
| `backend/src/main/java/.../infra/ai/exception/AiErrorCode.java` | `SERVICE_UNAVAILABLE` 추가 |
| `backend/src/main/java/.../global/exception/GlobalExceptionHandler.java` | `CallNotPermittedException` 핸들러 추가 |
| `backend/build.gradle.kts` | `resilience4j-circuitbreaker` 의존성 추가 |

## 상세

### ResilientAiClient 구현

```java
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class ResilientAiClient implements AiClient {

    private final OpenAiClient openAiClient;
    private final ClaudeApiClient claudeApiClient;

    @Override
    @CircuitBreaker(name = "openai-api", fallbackMethod = "generateQuestionsFallback")
    @RateLimiter(name = "openai-api")
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        return openAiClient.generateQuestions(request);
    }

    @Override
    @CircuitBreaker(name = "openai-api", fallbackMethod = "generateFollowUpFallback")
    @RateLimiter(name = "openai-api")
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        return openAiClient.generateFollowUpQuestion(request);
    }

    // --- Fallback: Claude API ---
    // CB CLOSED 상태: OpenAI 1회 재시도(총 2회) 실패 → Claude fallback
    // CB OPEN 상태: OpenAI 시도 생략 → Claude 직행 (CallNotPermittedException)

    private List<GeneratedQuestion> generateQuestionsFallback(
            QuestionGenerationRequest request, Exception e) {
        log.warn("[AI Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
        try {
            return claudeApiClient.generateQuestions(request);
        } catch (Exception fallbackEx) {
            log.error("[AI Fallback] Claude도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private GeneratedFollowUp generateFollowUpFallback(
            FollowUpGenerationRequest request, Exception e) {
        log.warn("[AI Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
        try {
            return claudeApiClient.generateFollowUpQuestion(request);
        } catch (Exception fallbackEx) {
            log.error("[AI Fallback] Claude도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
```

### Resilience4j 데코레이터 순서

```
요청 → [RateLimiter] → [CircuitBreaker] → OpenAiClient (수동 1회 재시도)
                              ↓ (OPEN 또는 실패)
                         fallback → ClaudeApiClient
```

호출 흐름:
1. **RateLimiter** — 초당 호출 수 확인. 초과 시 대기 또는 거부
2. **CircuitBreaker** — 상태 확인. OPEN이면 즉시 fallback (Claude 직행)
3. CLOSED면 → OpenAiClient 호출 (내부 1회 재시도, 총 2회)
4. 2회 모두 실패 → CB에 실패 1회 기록 + fallback (Claude)

> 참고: Retry는 Resilience4j `@Retry`가 아닌 OpenAiClient 내부 수동 for-loop.
> 재시도 1회만 하고 빠르게 fallback 전환하는 것이 목표이므로 가벼운 수동 구현.
> ClaudeApiClient는 기존 3회 재시도 유지 (fallback이므로 최대한 성공해야 함).

### ClaudeApiClient 리팩토링

- `implements AiClient` 제거
- `@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")` 유지
- 메서드 시그니처 동일 유지 (generateQuestions, generateFollowUpQuestion)
- `@RateLimiter(name = "claude-api")` 유지

### MockAiClient 조건 변경

```java
@ConditionalOnMissingBean(ResilientAiClient.class)  // 변경
public class MockAiClient implements AiClient {
```

두 API 키 모두 비어있으면:
- OpenAiClient 빈 생성 안 됨 → ResilientAiClient 빈 생성 안 됨 → MockAiClient 활성화

### AiErrorCode 추가

```java
SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_006",
    "AI 서비스가 일시적으로 사용 불가합니다. 잠시 후 다시 시도해주세요.");
```

### GlobalExceptionHandler 추가

```java
@ExceptionHandler(CallNotPermittedException.class)
protected ResponseEntity<ApiResponse<Void>> handleCircuitBreakerOpen(
        CallNotPermittedException e) {
    log.warn("서킷브레이커 OPEN — 호출 차단: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ApiResponse.error("AI_006", "AI 서비스가 일시적으로 사용 불가합니다."));
}
```

### build.gradle.kts 의존성 추가

```kotlin
implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
```

## 담당 에이전트

- Implement: `backend` — ResilientAiClient + 리팩토링 + 예외 처리
- Review: `architect-reviewer` — Composite 패턴 구조, 데코레이터 순서, fallback 로직

## 검증

- `./gradlew build` 성공
- ResilientAiClient가 `@Primary` AiClient 빈으로 등록 확인
- 서비스 계층(InterviewService, FreshQuestionProvider 등) 코드 변경 없음 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
