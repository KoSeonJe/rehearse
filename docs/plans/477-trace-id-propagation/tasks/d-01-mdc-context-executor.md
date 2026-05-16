# Task D1 — MdcContextExecutor 신설 (Domain Unit 포함)

> **PR**: PR-D
> **영역**: BE
> **선행 Task**: PR-A 머지 + dev/prod 배포 완료

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/global/util/MdcContextExecutor.java` — **신규**. `Executor` 데코레이터.
- `backend/src/test/java/com/rehearse/api/global/util/MdcContextExecutorTest.java` — **신규** (Domain Unit).

---

## 핵심 로직

```java
public class MdcContextExecutor implements Executor {
    private final Executor delegate;

    private MdcContextExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    public static Executor wrap(Executor delegate) {
        return new MdcContextExecutor(delegate);
    }

    @Override
    public void execute(Runnable command) {
        Map<String, String> snapshot = MDC.getCopyOfContextMap();
        delegate.execute(() -> {
            Map<String, String> prev = MDC.getCopyOfContextMap();
            if (snapshot != null) {
                MDC.setContextMap(snapshot);
            } else {
                MDC.clear();
            }
            try {
                command.run();
            } finally {
                MDC.clear();
                if (prev != null) {
                    MDC.setContextMap(prev);
                }
            }
        });
    }
}
```

---

## 테스트

**카테고리**: Domain Unit (`DomainUnitSupport` 미사용 — 순수 객체)

```java
class MdcContextExecutorTest {
    @Test @DisplayName("호출자 MDC snapshot 이 task thread 에 복원된다")
    void restores_mdc_snapshot_in_task_thread() throws Exception {
        Executor delegate = Executors.newSingleThreadExecutor();
        Executor executor = MdcContextExecutor.wrap(delegate);
        MDC.put("traceId", "abc12345");
        CompletableFuture<String> result = new CompletableFuture<>();
        executor.execute(() -> result.complete(MDC.get("traceId")));
        assertThat(result.get(1, SECONDS)).isEqualTo("abc12345");
    }

    @Test @DisplayName("호출자 MDC 가 비어있을 때 task thread MDC 도 비어있다")
    void clears_mdc_in_task_when_callerEmpty() { ... }

    @Test @DisplayName("task 종료 후 thread MDC 가 정리된다")
    void clears_mdc_after_task_completes() { ... }

    @Test @DisplayName("task 예외 발생 시에도 thread MDC 가 정리된다")
    void clears_mdc_on_task_exception() { ... }
}
```

---

## 의존

- 선행: 없음 (PR-A 머지 후 진입)
- 외부: SLF4J MDC

---

## Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.global.util.MdcContextExecutorTest"`
- 통과 기준: 4 케이스 green

---

## 커밋 메시지 (예상)

```
feat(BE): MdcContextExecutor 신설 - MDC 전파 Executor 데코레이터
```
