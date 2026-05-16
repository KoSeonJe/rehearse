# Task D2 — 3 Executor bean wrap

> **PR**: PR-D
> **영역**: BE
> **선행 Task**: D1

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/global/config/AsyncConfig.java` — **변경**. `vtExecutor` bean = `MdcContextExecutor.wrap(new DelegatingSecurityContextExecutor(VT))`.
- `backend/src/main/java/com/rehearse/api/global/config/RubricScoringExecutorConfig.java` — **변경**. 동일 wrap.
- `backend/src/main/java/com/rehearse/api/global/config/SessionFeedbackExecutorConfig.java` — **변경**. 동일 wrap.

---

## 핵심 로직

```java
// AsyncConfig.java (변경부)
@Configuration
public class AsyncConfig {
    @Bean
    public Executor vtExecutor() {
        Executor base = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("vt-async-", 0).factory()
        );
        return MdcContextExecutor.wrap(new DelegatingSecurityContextExecutor(base));
    }
}

// RubricScoringExecutorConfig.java (변경부)
@Bean
public Executor rubricScoringExecutor() {
    Executor base = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("rubric-scoring-", 0).factory()
    );
    return MdcContextExecutor.wrap(base);
}

// SessionFeedbackExecutorConfig.java (변경부)
@Bean
public Executor sessionFeedbackExecutor() {
    Executor base = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("session-feedback-", 0).factory()
    );
    return MdcContextExecutor.wrap(base);
}
```

> **주의**: `DelegatingSecurityContextExecutor` 와 `MdcContextExecutor` 순서 = MDC wrap **이 outer**. 그래야 SecurityContext + MDC 둘 다 전파 (snapshot 캡처 시점이 호출자 thread).

---

## 의존

- 선행: D1 (`MdcContextExecutor`)
- 외부: Spring `DelegatingSecurityContextExecutor`

---

## Verification Hook

- 명령: `./gradlew compileJava`
- 통과 기준: 컴파일 통과. 동작 검증은 D5 (통합 테스트).

---

## 커밋 메시지 (예상)

```
feat(BE): vtExecutor / rubricScoring / sessionFeedback executor MDC wrap
```
