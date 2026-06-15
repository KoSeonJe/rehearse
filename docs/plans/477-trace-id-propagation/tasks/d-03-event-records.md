# Task D3 — 이벤트 record 4종 traceId 필드 추가 + 발행자 6곳 동시 수정

> **PR**: PR-D
> **영역**: BE
> **선행 Task**: 없음 (D1/D2 와 병렬 가능. 단 D4 와 atomic — 같은 커밋)

---

## 변경 파일

### 이벤트 record (4종)
- `backend/src/main/java/com/rehearse/api/domain/interview/event/QuestionGenerationRequestedEvent.java` — **변경**. record 컴포넌트 첫 자리 `String traceId` 추가.
- `backend/src/main/java/com/rehearse/api/domain/interview/event/InterviewCompletedEvent.java` — **변경**. 동일.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/event/TurnCompletedEvent.java` — **변경**. 정적 팩토리 보유 시 시그니처 갱신.
- `backend/src/main/java/com/rehearse/api/domain/feedback/session/event/DeliveryEnrichmentRequestedEvent.java` — **변경**. 동일.

### 발행자 (6곳)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeTurnEventPublisher.java:50` — **변경**. 발행 직전 `MDC.get("traceId")` 캡처 → event 컴포넌트 주입.
- `backend/src/main/java/com/rehearse/api/domain/feedback/service/FeedbackService.java:48` — **변경**. `new DeliveryEnrichmentRequestedEvent(MDC.get("traceId"), interviewId)`.
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewCreationService.java:63` — **변경**. `new QuestionGenerationRequestedEvent(MDC.get("traceId"), ...)`.
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java:97` — **변경**. 동일.
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewCompletionService.java:55` — **변경**. `new InterviewCompletedEvent(MDC.get("traceId"), interviewId, LocalDateTime.now())`.
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpTransactionHandler.java:166` — **변경**. event 빌드 직전 traceId 주입.

> **주의**: record 컴포넌트 추가 = 호출부 컴파일 에러 강제 → 6곳 동시 수정 필수 (atomic 커밋).

---

## 핵심 로직

```java
// QuestionGenerationRequestedEvent.java (변경부)
public record QuestionGenerationRequestedEvent(
    String traceId,       // 신규 — 첫 컴포넌트
    Long interviewId,
    // 기존 필드 ...
) {}
```

```java
// 발행자 (예: InterviewCompletionService.java:55)
eventPublisher.publishEvent(
    new InterviewCompletedEvent(
        MDC.get("traceId"),   // 신규 캡처 — 6곳 모두 동일 패턴
        interviewId,
        LocalDateTime.now()
    )
);
```

### 헬퍼 도입 여부 결정 (본 plan 한정)

**채택**: `MDC.get("traceId")` **6곳 직접 호출** (helper 미도입).

**사유**:
- 본 plan 범위 = 6곳 동일 1줄 패턴. helper 도입 시 추가 추상화 1단계 (`TraceContext.current()` 등) — 조기 추상화 회피 (simplicity.md §"단발성 코드에 추상화 / 인터페이스 / 옵션 만들지 말 것").
- 회귀 가드는 **이벤트 record 컴포넌트 컴파일 강제** + **listener MDC 복원 통합 테스트** (D5) 가 담당. 발행 site 누락은 컴파일 에러로 즉시 검출.
- 패턴 위반 의심 시 `grep -rn "new \(QuestionGenerationRequested\|InterviewCompleted\|TurnCompleted\|DeliveryEnrichmentRequested\)Event" backend/src/main` 1회 grep 로 회귀 가시화 가능.

**향후 helper 추출 조건**: 동일 패턴 호출이 **9곳 이상** (3회 반복 룰: 6 + 3) 발생 시 `TraceContext.currentTraceId()` 추출 검토. 본 plan 한정 미도입.

> **MDC.get 이 null 인 경우** — 정상 운영에서는 발생 X (TraceIdFilter / InternalApiKeyFilter 진입 보장). null 허용 (record 컴포넌트). listener 진입 시 null 이면 fallback UUID 처리 (D4 에서 결정).

---

## 의존

- 선행: 없음
- 외부: SLF4J MDC

---

## Verification Hook

- 명령: `./gradlew compileJava` (호출부 누락 시 컴파일 에러)
- 통과 기준: 전체 컴파일 통과 + 기존 발행자 6곳 + 호출부 동시 갱신
- 추가 검증: `grep -rn "new QuestionGenerationRequestedEvent\|new InterviewCompletedEvent\|new TurnCompletedEvent\|new DeliveryEnrichmentRequestedEvent" backend/src/main` → 6곳 모두 traceId 인자 첫 자리

---

## 커밋 메시지 (예상)

```
feat(BE): ApplicationEvent 4종 traceId 필드 + 발행자 6곳 캡처
```
