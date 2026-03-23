# Plan 03: AsyncConfig 정리 (VT executor 전환)

> 상태: Draft
> 작성일: 2026-03-23

## Why

VT 활성화 시 Spring Boot가 자동으로 Virtual Thread executor를 사용하므로, 커스텀 Platform Thread 풀(`questionGenerationExecutor`, `questionSubTaskExecutor`)이 불필요하다. 오히려 Platform Thread 풀이 VT의 동시성 이점을 제한한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/.../global/config/AsyncConfig.java` | 두 executor Bean 제거, `@EnableAsync`만 유지 |
| `backend/src/main/java/.../interview/service/QuestionGenerationEventHandler.java` | `@Async("questionGenerationExecutor")` → `@Async` |
| `backend/src/main/java/.../interview/service/QuestionGenerationService.java` | `questionSubTaskExecutor` 제거 → `Executors.newVirtualThreadPerTaskExecutor()` |

## 상세

### AsyncConfig.java

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    // VT 환경: Spring Boot 3.4가 자동으로 VT executor 사용
}
```

### QuestionGenerationEventHandler.java (line 18)

```java
// 변경 전
@Async("questionGenerationExecutor")

// 변경 후
@Async
```

### QuestionGenerationService.java

- 생성자에서 `@Qualifier("questionSubTaskExecutor") Executor questionSubTaskExecutor` 파라미터/필드 제거
- 필드 추가: `private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();`
- `CompletableFuture.supplyAsync(..., questionSubTaskExecutor)` → `CompletableFuture.supplyAsync(..., virtualExecutor)`

## 담당 에이전트

- Implement: `backend` — executor 전환
- Review: `architect-reviewer` — VT executor 정합성

## 검증

- `questionGenerationExecutor` Bean 제거 확인
- `questionSubTaskExecutor` Bean 제거 확인
- `@Async` 어노테이션에 qualifier 없음 확인
- 질문 생성 기능 정상 동작 확인 (기존 테스트 통과)
- `progress.md` 상태 업데이트 (Task 3 → Completed)
