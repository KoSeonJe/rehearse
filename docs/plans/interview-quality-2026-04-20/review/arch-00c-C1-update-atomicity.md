---
id: arch-00c-C1-update-atomicity
severity: critical
category: architecture
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# `InterviewRuntimeStateStore.update()` atomicity 계약 미명시

## 문제

`update(Long interviewId, Consumer<InterviewRuntimeState> mutator)` 가 "atomic mutation" 으로 오해될 수 있으나 실제로는 동일 interviewId 에 대한 read-modify-write 경합을 보호하지 않는다.

## 원인

`InterviewRuntimeStateStore.java:43` 의 `update()` 는 내부적으로 `cache.get(k, mappingFunction)` 으로 state 조회 후 `mutator.accept(state)` 를 호출한다. Caffeine 의 `get(k, mappingFunction)` 은 **mappingFunction 만** 키별 직렬화하며, 반환된 state 에 대한 이후 mutation 은 보호하지 않는다.

POJO 내부 필드가 `ConcurrentHashMap` / `CopyOnWriteArrayList` / `AtomicInteger` 라 단순 add / increment 는 안전하지만, **합성 변경** (예: `state.setCurrentLevel(promote(state.getCurrentLevel()))`, 점수 누적, turnAnalysisCache 의 조건부 put) 은 race 로 값 유실이 발생한다.

## 발생 상황

- **언제**: 동일 interview 세션에서 사용자가 빠르게 연속 답변 전송 → 여러 스레드가 거의 동시에 `update()` 호출
- **누가**: `plan-08` (RubricScorer) 가 점수 누적 로직을 `update()` 안에서 돌릴 때 가장 위험
- **파장**: 점수 누락 → 세션 종합 피드백 왜곡 → 사용자에게 잘못된 평가 제공. 로그/테스트로 감지 어려움

## 해결 방법

`InterviewLockService` 를 생성자 주입 → `update()` 내부에서 `withLock()` 호출.

```java
// Before
public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) {
    InterviewRuntimeState state = cache.get(interviewId, k -> new InterviewRuntimeState());
    mutator.accept(state);
}

// After
public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) {
    lockService.withLock(interviewId, () -> {
        InterviewRuntimeState state = cache.get(interviewId, k -> new InterviewRuntimeState());
        mutator.accept(state);
        return null;
    });
}
```

호출자가 이미 `withLock` 안에 있어도 `InterviewLockService` 가 `ReentrantLock` 기반이라 재진입 허용 → 데드락 없음.

**대안(기각)**: javadoc 경고만 추가. 근본 해결 아니고 호출자가 잊기 쉬움.

## 결과

- 수정: `backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeStateStore.java`
- javadoc: "동일 `interviewId` 에 대한 `update()` 실행은 `InterviewLockService` 로 직렬화된다"
- 테스트: `InterviewRuntimeStateStoreTest` 에 "read-modify-write 합성 변경이 race 없이 직렬화" 케이스 추가
- 후속 plan-08 RubricScorer 는 별도 락 없이 `store.update()` 만 호출하면 됨
