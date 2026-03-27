# Async Virtual Thread Executor — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-27

## Why

### 1. Why? — 어떤 문제를 해결하는가?

현재 `spring.threads.virtual.enabled=true`로 Tomcat 전체가 Virtual Thread 위에서 동작한다. 하지만 부하테스트 결과, **TX 분리가 처리량 11배 개선의 핵심**이었고 VT의 효과는 66 req/s 이상에서만 유의미했다. 현재 서비스 트래픽(GPT-4o-mini 기준 ~8 req/s)에서 글로벌 VT는 오버킬이다.

글로벌 VT 활성화의 리스크:
- HikariCP 6.x 강제 업그레이드 필요 (Spring Boot 번들 5.x와 버전 불일치)
- VT 환경에서 `synchronized` 블록 pinning 추적 부담
- 라이브러리 호환성 이슈 표면적 증가 (모든 요청이 VT)
- 디버깅/프로파일링 시 스레드 모델 혼란

또한 현재 두 가지 외부 AI API 호출이 **Tomcat PT를 장시간 블로킹**하고 있다:
- `createInterview` → `@Async` 이벤트로 비동기 처리 중이나, 이벤트+리스너 패턴이 간접적
- `generateFollowUp` → **동기 API로 3초간 PT 점유** (66 req/s 천장의 원인)

### 2. Goal — 구체적인 결과물과 성공 기준

| 지표 | 현재 | 목표 |
|------|------|------|
| Tomcat 스레드 모델 | Virtual Thread (글로벌) | **Platform Thread** |
| 질문 생성 (30-60초) | `@Async` 이벤트 리스너 (Spring 자동 VT) | **`@Async("vtExecutor")` 이벤트 리스너 (명시적 VT)** |
| 후속질문 생성 (3초) | 동기 (VT) | **동기 유지 (PT)** |
| HikariCP 버전 | 6.2.1 | 6.2.1 (유지 — `@Async` VT→`@Transactional` DB 경로) |
| FE 변경 | - | **없음** |

성공 기준:
- `spring.threads.virtual.enabled=false`로 전환
- 질문 생성: `@Async` → `@Async("vtExecutor")` 명시적 VT executor 지정 (이벤트 패턴 유지)
- 후속질문 생성: 동기 유지 (현재 코드 변경 없음)
- 기존 테스트 전체 통과
- FE 코드 변경 없음

### 3. Evidence — 근거 데이터

- **부하테스트 결과** (`docs/plans/virtual-thread-migration/load-test-results.md`):
  - TX 분리만으로 처리량 2.7→31.1 req/s (11배 증가)
  - PT 한계: 200 스레드 / 3초 블로킹 = 66 req/s 천장
  - VT 도입 시 응답시간 46% 단축 (5.81s→3.11s), dropped 0건
- **현재 develop 코드**:
  - TX 분리 이미 완료 (`FollowUpTransactionHandler`, `Propagation.NOT_SUPPORTED`)
  - `QuestionGenerationService`는 이미 로컬 VT executor 사용
  - `generateFollowUp`은 동기 API → Tomcat PT 3초 점유
- **Spring MVC CompletableFuture 지원**:
  - Controller에서 `CompletableFuture<ResponseEntity<>>` 반환 시 Servlet 3.0 Async 자동 활성화
  - Tomcat PT 즉시 반환, future 완료 시 아무 PT가 응답 전송
  - FE 관점에서 일반 HTTP 요청/응답과 동일 (투명)

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 선택 | 대안 | 대안 제외 이유 |
|------|------|----------------|
| 선택적 VT (`enabled=false` + 명시적 executor) | 글로벌 VT 유지 (`enabled=true`) | 현재 트래픽에서 불필요, 리스크 표면적 확대 |
| `@Async("vtExecutor")` 이벤트 유지 | CompletableFuture.runAsync 직접 호출 | `@TransactionalEventListener(AFTER_COMMIT)` 보장 유지 필요, 이벤트 패턴이 트랜잭션 안전 |
| 후속질문 동기 유지 | CompletableFuture 반환 | 3초는 PT 200 스레드로 충분, 불필요한 복잡도 회피 |
| HikariCP 6.2.1 유지 | 5.x로 다운그레이드 | VT→@Transactional 경로에서 pinning 발생 |

## 아키텍처

### 변경 전

```
[질문 생성]
  POST /interviews → [VT] createInterview() → 이벤트 발행 → 201 즉시 반환
                        ↓ @Async (Spring VT executor)
                     QuestionGenerationEventHandler → QuestionGenerationService
                        → CompletableFuture (로컬 VT executor)
                     FE: 질문 생성 상태 폴링

[후속질문 생성]
  POST /follow-up → [VT] generateFollowUp() → STT(1s) + AI(2s) + DB → 200 응답
```

### 변경 후

```
[질문 생성]
  POST /interviews → [PT] createInterview()
    → 면접 저장 (동기)
    → 이벤트 발행 → 201 즉시 반환 (질문 없음)
  → [트랜잭션 커밋]
  → @Async("vtExecutor") → [VT] QuestionGenerationEventHandler
    → questionGenerationService.generateQuestions() (30-60초, 백그라운드)
  FE: 질문 생성 상태 폴링 (기존과 동일)

[후속질문 생성]
  POST /follow-up → [PT] generateFollowUp() → STT(1s) + AI(2s) + DB → 200 응답
  동기 유지, 변경 없음
```

### VT 사용 경로 (변경 후)

| 경로 | 스레드 모델 | PT 점유 | DB 접근 |
|------|-----------|---------|---------|
| Tomcat 요청 처리 | **PT** | 요청 처리 동안 | O |
| 질문 생성 (CompletableFuture fire-and-forget) | **VT** | X (PT 반환됨) | O (`startGeneration`, `saveResults`) |
| 질문 병렬 제공 (로컬 executor) | VT | X | X (메모리 + 외부 API) |
| 후속질문 생성 (동기) | **PT** | ~3초 | O (`loadFollowUpContext`, `saveFollowUpResult`) |
| 기타 동기 API (조회, 상태변경) | PT | 짧음 (ms) | O |

## Scope

### In
- `spring.threads.virtual.enabled` → `false`
- `AsyncConfig`에 명시적 VT executor Bean 정의
- `QuestionGenerationEventHandler`: `@Async` → `@Async("vtExecutor")` (이벤트 패턴 유지)
- 기존 테스트 통과 검증

### Out
- `generateFollowUp` 변경 (동기 유지, 현재 코드 그대로)
- `InterviewService.createInterview()` 변경 (이벤트 발행 패턴 유지)
- `QuestionGenerationEventHandler` 삭제 (유지)
- `QuestionGenerationRequestedEvent` 삭제 (유지)
- `QuestionGenerationService` 로컬 executor 변경 (이미 VT, 변경 불필요)
- HikariCP 다운그레이드 (VT→DB 경로 때문에 6.x 유지)
- FE 코드 변경 (HTTP 계약 동일)
- `bootRun` JVM args 제거 (`-Djdk.tracePinnedThreads=short` 유지)
- 부하테스트 재실행

## 제약조건

- Java 21, Spring Boot 3.4.3
- `@Async` void 메서드의 예외는 `AsyncUncaughtExceptionHandler`로 전파됨 (`@ExceptionHandler`가 아님). 현재 `QuestionGenerationEventHandler`가 자체 try-catch + `failGeneration()`으로 처리하므로 문제없음
- `@Scheduled` 작업 (`AnalysisScheduler` 등)도 PT에서 실행됨. 짧은 DB 작업이므로 영향 없음
- `QuestionGenerationService`는 Spring `@Async`가 아닌 로컬 `ExecutorService` 사용 → 영향 없음 (VT executor 2개 공존은 의도적: Spring 관리 Bean vs 로컬 생명주기)
