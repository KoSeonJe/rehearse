# Plan 03: 질문 생성 @Async VT Executor 적용

> 상태: Draft
> 작성일: 2026-03-27

## Why

질문 생성은 현재 `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` 이벤트 패턴으로 동작한다. 이 패턴은 트랜잭션 커밋 후 실행을 보장하므로 유지한다. 변경은 `@Async` executor를 명시적 VT executor로 지정하는 것뿐이다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/interview/service/QuestionGenerationEventHandler.java` | `@Async` → `@Async("vtExecutor")` |

## 상세

### QuestionGenerationEventHandler.java

```java
@Async("vtExecutor")  // 명시적 VT executor 지정
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleQuestionGenerationEvent(QuestionGenerationRequestedEvent event) {
    // 기존 로직 그대로
}
```

변경: `@Async` → `@Async("vtExecutor")` (1줄)

### 유지하는 것

| 항목 | 이유 |
|------|------|
| `@TransactionalEventListener(AFTER_COMMIT)` | 면접 저장 트랜잭션 커밋 후 실행 보장 |
| `QuestionGenerationRequestedEvent` | 이벤트 객체 유지 |
| `InterviewService.createInterview()` | 이벤트 발행 로직 변경 없음 |
| `retryQuestionGeneration()` | 동일하게 이벤트 발행 (변경 없음) |
| 에러 처리 | Handler에서 catch → `failGeneration()` (변경 없음) |

### 동작 흐름 (변경 후)

```
[FE] POST /interviews
  → [PT] createInterview() → 면접 저장 → 이벤트 발행 → 201 즉시 반환
  → [트랜잭션 커밋]
  → @Async("vtExecutor") → [VT] handleQuestionGenerationEvent()
    → questionGenerationService.generateQuestions() (30-60초)
[FE] 질문 생성 상태 폴링 (기존과 동일)
```

변경 전과 HTTP 동작 완전 동일. 내부적으로 Spring 자동 VT executor → 명시적 vtExecutor로만 바뀜.

## 담당 에이전트

- Implement: `backend` — @Async qualifier 적용
- Review: `code-reviewer` — 누락된 @Async 사용처 없는지 확인

## 검증

- 면접 생성 → 질문 생성 비동기 완료 확인
- `retryQuestionGeneration` 정상 동작 확인
- VT에서 실행되는지 로그 확인 (`Thread.currentThread().isVirtual()`)
- 기존 테스트 통과
- `progress.md` 상태 업데이트 (Task 3 → Completed)
