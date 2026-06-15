# Task D4 — listener 4 메서드 (3 클래스) 진입 첫 줄 MDC 복원

> **PR**: PR-D
> **영역**: BE
> **선행 Task**: D3 (event record 의 traceId 필드 존재)

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/interview/event/QuestionGenerationEventHandler.java:25` — **변경**. listener 진입 첫 줄 `MDC.put("traceId", event.traceId())`. finally `MDC.remove("traceId")`.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScoringEventListener.java:34` — **변경**. 동일.
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackEventListener.java:24` — **변경**. 동일 (메서드 1).
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackEventListener.java:40` — **변경**. 동일 (메서드 2).

---

## 핵심 로직

```java
// QuestionGenerationEventHandler.java (변경부)
@Async("vtExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handle(QuestionGenerationRequestedEvent event) {
    String traceId = event.traceId();
    if (traceId == null) {
        traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.warn("이벤트 페이로드 traceId 부재 - fallback uuid 사용: event={}", event.getClass().getSimpleName());
    }
    MDC.put("traceId", traceId);
    try {
        // 기존 로직
    } finally {
        MDC.remove("traceId");
    }
}
```

> **신뢰 기준**: 이벤트 페이로드 `event.traceId()` 가 우선 (tech-spec 사용자 결정). MdcContextExecutor wrap (D2) 은 보조 — wrap 누락 시에도 페이로드 필드로 복원.

> **null 정책**: 페이로드 null 시 WARN + fallback UUID (operational defect 시그널). reject X.

---

## 의존

- 선행: D3 (event record traceId 필드)
- 외부: SLF4J MDC

---

## Verification Hook

- 명령: `./gradlew compileJava`
- 통과 기준: 컴파일 통과. 동작 검증은 D5.

---

## 커밋 메시지 (예상)

```
feat(BE): @TransactionalEventListener 진입 MDC traceId 복원
```
