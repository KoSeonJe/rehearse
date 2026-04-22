---
id: arch-00c-C2-lock-transaction-ordering
severity: critical
category: architecture
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# Lock / Transaction 순서 규약 부재

## 문제

`InterviewLockService.withLock()` 이 `@Transactional` 메서드와 어떤 순서로 조합되어야 하는지 프로젝트 전체 규약이 없다. 잘못된 조합은 cross-stripe deadlock 또는 lock-wait-timeout 을 유발한다.

## 원인

`InterviewLockService.java` 는 `ReentrantLock[256]` 배열에 `interviewId % 256` 으로 매핑 — 서로 다른 interviewId 가 동일 stripe 로 매핑될 수 있다 (예: `id=1` 과 `id=257`). 이 상태에서:

- **Thread A**: `withLock(1)` 획득 → 내부에서 `UPDATE interview WHERE id=1` 로 DB row lock 획득
- **Thread B**: `UPDATE interview WHERE id=257` 으로 DB row lock 획득 → `withLock(257)` 대기 (stripe 동일)

JVM lock 과 DB row lock 이 서로 다른 순서로 획득되면 circular wait 성립.

후속 plan(05/06/08/09) 모두 `withLock + @Transactional` 조합을 쓸 예정인데 규약이 없다.

## 발생 상황

- **언제**: 동시 세션 수가 늘어 `interviewId % 256` collision 이 실제로 발생하는 운영 환경
- **누가**: 후속 plan 구현자 — 규약 없이 각자 다른 패턴을 채택하면 사후 통합 시 cross-cutting 버그
- **파장**: 간헐적 요청 타임아웃, 디버깅 난이도 극상 (로컬 재현 어려움)

## 해결 방법

두 가지 방어:

1. **규약 문서화** — `STATE_DESIGN.md` 에 "Lock Acquisition Contract" 섹션 추가
   - `withLock` 은 항상 `@Transactional` **바깥**에서 획득 (lock outer → txn inner)
   - `withLock` 안에서는 **단일 `interviewId`** 의 DB 작업만 수행
   - 다중 interview 업데이트는 `withLock` 없이 DB transaction isolation 에 의존

2. **`tryLock(timeout)` 오버로드** — 무한 block 방지

```java
public <T> T tryLock(Long interviewId, Duration timeout, Supplier<T> action) {
    Lock lock = stripedLock[stripeIndex(interviewId)];
    try {
        if (!lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new LockAcquisitionException(interviewId, timeout);
        }
        try { return action.get(); } finally { lock.unlock(); }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new LockAcquisitionException(interviewId, e);
    }
}
```

**대안(기각)**: stripe 크기를 `Integer.MAX_VALUE` 로. 메모리 낭비 + collision 확률만 줄일 뿐 근본 해결 아님.

## 결과

- 수정: `docs/plans/interview-quality-2026-04-20/STATE_DESIGN.md` — "Lock Acquisition Contract" 섹션 추가
- 수정: `backend/src/main/java/com/rehearse/api/domain/interview/lock/InterviewLockService.java` — `tryLock(Long, Duration, Supplier<T>)` 추가 + javadoc 규약
- 신규 예외: `LockAcquisitionException`
- 테스트: `InterviewLockServiceTest` 에 timeout 시나리오 케이스 추가
- 후속 plan 은 STATE_DESIGN 의 Lock Contract 를 참조
