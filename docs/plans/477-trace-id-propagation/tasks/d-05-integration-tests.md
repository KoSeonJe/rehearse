# Task D5 — Executor MDC 전파 + listener MDC 복원 통합 테스트 + AGENTS.md 안내

> **PR**: PR-D
> **영역**: BE
> **선행 Task**: D1, D2, D3, D4

---

## 변경 파일

### Executor MDC 전파 (Service Integration)
- `backend/src/test/java/com/rehearse/api/global/config/VtExecutorMdcPropagationTest.java` — **신규**.
- `backend/src/test/java/com/rehearse/api/global/config/RubricScoringExecutorMdcPropagationTest.java` — **신규**.
- `backend/src/test/java/com/rehearse/api/global/config/SessionFeedbackExecutorMdcPropagationTest.java` — **신규**.

### listener MDC 복원 (Service Integration)
- `backend/src/test/java/com/rehearse/api/domain/interview/event/QuestionGenerationEventHandlerIntegrationTest.java` — **신규 또는 확장**.
- `backend/src/test/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListenerIntegrationTest.java` — **신규 또는 확장**.
- `backend/src/test/java/com/rehearse/api/domain/feedback/session/SessionFeedbackEventListenerIntegrationTest.java` — **신규 또는 확장** (2 메서드 검증).

### 안내 문서
- `backend/AGENTS.md` — **변경**. "신규 Executor bean 추가 시 MdcContextExecutor wrap + Mdc 전파 통합 테스트 1개 추가" 1줄 명시 (회귀 가드).

---

## 핵심 로직

### Executor MDC 전파 통합 테스트 패턴

```java
@SpringBootTest
class VtExecutorMdcPropagationTest extends ServiceIntegrationSupport {

    @Autowired
    @Qualifier("vtExecutor")
    private Executor executor;

    @Test
    @DisplayName("호출자 traceId 가 task thread 에 복원된다")
    void 호출자_traceId_가_task_thread_에_복원된다() throws Exception {
        MDC.put("traceId", "abc12345");
        CompletableFuture<String> result = new CompletableFuture<>();
        executor.execute(() -> result.complete(MDC.get("traceId")));
        assertThat(result.get(1, TimeUnit.SECONDS)).isEqualTo("abc12345");
    }
}
```

3 executor 동일 패턴 (`vtExecutor` / `rubricScoringExecutor` / `sessionFeedbackExecutor`).

### listener MDC 복원 통합 테스트 패턴

```java
@SpringBootTest
class RubricScoringEventListenerIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("listener 가 페이로드 traceId 를 MDC 에 복원한다")
    void listener_가_payload_의_traceId_를_MDC_에_복원한다() {
        // 호출자 MDC 와 별개로 페이로드 traceId 가 신뢰 기준
        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        // listener 가 호출 시점 MDC 캡처할 수 있도록 spy / log captor 활용
        // ...
        eventPublisher.publishEvent(new TurnCompletedEvent("payload-trace", ...));
        // AFTER_COMMIT + Async 종료 대기 (CountDownLatch / Awaitility)
        assertThat(capturedTraceId.get()).isEqualTo("payload-trace");
    }
}
```

> **주의**: `@TransactionalEventListener(AFTER_COMMIT)` 는 트랜잭션 커밋 후 실행 → 테스트에서 `TransactionTemplate` 으로 명시 트랜잭션 + 비동기 종료 동기화 필요.

### 비동기 동기화 패턴 (채택: `CountDownLatch`)

**채택**: `java.util.concurrent.CountDownLatch` + `AtomicReference` (JDK 표준). Awaitility / LogCaptor 의존성 추가 없음.

**사유**:
- `backend/build.gradle.kts` 점검 (line 26-108): Awaitility / LogCaptor 의존성 부재. 추가 도입 회피 (simplicity.md §"단발성 코드에 추상화 / 인터페이스 / 옵션 만들지 말 것").
- 기존 BE 테스트 정합: `GlobalRateLimiterFilterTest` / `QuestionGenerationLockTest` / `InterviewRuntimeStateCacheGetOrInitTest` 등 다수가 `CountDownLatch` 패턴 사용 — 본 plan 도 동일 정합.
- listener 진입 시점에 latch.countDown + MDC 값 캡처. 호출자 측에서 `latch.await(1, TimeUnit.SECONDS)` 대기 후 단언.

```java
@SpringBootTest
class RubricScoringEventListenerIntegrationTest extends ServiceIntegrationSupport {

    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;

    // listener 가 호출 시점 MDC 캡처하도록 spy 또는 테스트 listener bean 사용
    @MockBean(answer = Answers.CALLS_REAL_METHODS) RubricScoringEventListener listener;
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<String> captured = new AtomicReference<>();

    @Test
    @DisplayName("listener 가 페이로드 traceId 를 MDC 에 복원한다")
    void listener_가_payload_의_traceId_를_MDC_에_복원한다() throws Exception {
        doAnswer(inv -> {
            captured.set(MDC.get("traceId"));
            latch.countDown();
            return inv.callRealMethod();
        }).when(listener).onTurnCompleted(any());

        transactionTemplate.execute(s -> {
            eventPublisher.publishEvent(new TurnCompletedEvent("payload-trace", /* ... */));
            return null;
        });

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(captured.get()).isEqualTo("payload-trace");
    }
}
```

> 기존 listener 가 `@MockBean(answer = CALLS_REAL_METHODS)` 패턴과 호환되지 않으면 별도 테스트 listener bean (`@TestConfiguration` 안 `@EventListener`) 등록 후 latch / capture 보유. backend agent 가 구현 시 listener 클래스 구조에 맞춰 선택.

### AGENTS.md 추가 문구

```markdown
## 핵심 룰

- ...
- **신규 Executor bean 추가 시**: `MdcContextExecutor.wrap(...)` 으로 감싸고 Mdc 전파 통합 테스트 1개 추가 (`global/config/*MdcPropagationTest`). 회귀 가드.
```

---

## 의존

- 선행: D1, D2, D3, D4
- 외부: Spring `@SpringBootTest`, Awaitility, AssertJ

---

## Verification Hook

- 명령:
  - `./gradlew test --tests "com.rehearse.api.global.config.*MdcPropagationTest"` (3 case)
  - `./gradlew test --tests "*IntegrationTest"` (listener 4 케이스)
  - `./gradlew build` (전체 빌드)
- 통과 기준: 모든 케이스 green
- 관찰 가능 동작: dev 종단 시나리오 재실행 → hop 6 (비동기 리스너 진입 로그) 호출자 traceId 일치 확인

---

## 커밋 메시지 (예상)

```
test(BE): Executor MDC 전파 + listener traceId 복원 통합 테스트
docs(BE): 신규 Executor bean 추가 시 wrap 안내
```
