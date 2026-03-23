# Plan 04: Resilience4j RateLimiter 적용

> 상태: Draft
> 작성일: 2026-03-23

## Why

VT가 동시성 제한을 없애므로, 외부 API(Claude, Whisper)에 대한 동시 호출 수를 명시적으로 제어해야 한다. 기존에는 스레드 풀 크기(max=5)가 암묵적 동시성 제한 역할을 했지만, VT 환경에서는 RateLimiter로 대체한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | resilience4j + AOP 의존성 추가 |
| `backend/src/main/resources/application-prod.yml` | RateLimiter 설정 추가 |
| `backend/src/main/java/.../infra/ai/ClaudeApiClient.java` | `@RateLimiter` 어노테이션 적용 (2개 메서드) |
| `backend/src/main/java/.../infra/ai/WhisperService.java` | `@RateLimiter` 어노테이션 적용 |
| `backend/src/main/java/.../global/exception/GlobalExceptionHandler.java` | `RequestNotPermitted` 예외 핸들러 추가 |

## 상세

### 의존성 (build.gradle.kts)

```kotlin
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
implementation("org.springframework.boot:spring-boot-starter-aop")
```

### 설정 (application-prod.yml)

```yaml
resilience4j:
  ratelimiter:
    instances:
      claude-api:
        limitForPeriod: 20
        limitRefreshPeriod: 1s
        timeoutDuration: 60s
      whisper-api:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 60s
```

### 어노테이션 적용

- `ClaudeApiClient.generateQuestions()` → `@RateLimiter(name = "claude-api")`
- `ClaudeApiClient.generateFollowUpQuestion()` → `@RateLimiter(name = "claude-api")`
- `WhisperService.transcribe()` → `@RateLimiter(name = "whisper-api")`

### 예외 핸들러 (GlobalExceptionHandler)

```java
@ExceptionHandler(RequestNotPermitted.class)
protected ResponseEntity<ErrorResponse> handleRateLimited(
        RequestNotPermitted e, HttpServletRequest request) {
    log.warn("Rate limit exceeded: uri={}", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(ErrorResponse.of(429, "RATE_LIMITED",
            "현재 요청이 많습니다. 잠시 후 다시 시도해주세요."));
}
```

### 주의사항

- `@RateLimiter`는 구현 클래스에 적용 (인터페이스 X)
- ClaudeApiClient 내부 retry 로직과 RateLimiter는 독립적으로 동작 (RateLimiter가 먼저 검사)

## 담당 에이전트

- Implement: `backend` — 의존성 + 어노테이션 + 예외 핸들러
- Review: `code-reviewer` — AOP 프록시 동작, 어노테이션 위치 검증

## 검증

- `@RateLimiter` 어노테이션 존재 확인 (ClaudeApiClient 2개, WhisperService 1개)
- `RequestNotPermitted` 예외 핸들러 존재 확인
- `./gradlew build` 성공
- `progress.md` 상태 업데이트 (Task 4 → Completed)
