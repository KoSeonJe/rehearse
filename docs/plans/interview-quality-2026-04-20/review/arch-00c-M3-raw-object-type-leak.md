---
id: arch-00c-M3-raw-object-type-leak
severity: major
category: architecture
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# `InterviewRuntimeState` 의 raw `Object` 타입 노출

## 문제

`InterviewRuntimeState.java:17-18` — `Map<Long, Object> turnAnalysisCache`, `Object resumeSkeletonCache`. 타입 안전성이 없고, 후속 plan 이 임의 타입을 끼워 넣으면 cross-plan 호환성이 깨진다.

## 원인

plan-05 (ResumeSkeleton), plan-08 (RubricScorer) 가 아직 구현되지 않아 **정확한 타입을 모르는 채** 선제적으로 필드를 둔 결과. 당장 컴파일은 되지만 후속 plan 이 합의 없이 각자 다른 DTO 를 밀어넣는 순간 런타임 ClassCastException + 직렬화(향후 Redis 이관 시) 실패.

## 발생 상황

- **언제**: plan-05 / plan-08 구현 시점, 또는 multi-node 확장 시 Redis 이관 시도 시
- **누가**: 후속 plan 구현자
- **파장**: 인터페이스 계약 부재로 cross-plan 통합 테스트 실패. 직렬화 장애 발생 시 운영에서야 발견

## 해결 방법

마커 인터페이스 2개를 plan-00c 에서 미리 정의. 후속 plan 은 이를 구현한 concrete 타입을 만든다.

```java
// 신규
package com.rehearse.api.domain.interview.runtime;

public interface TurnAnalysis {
    long turnId();
}

public interface CachedResumeSkeleton {
    String fileHash();
}

// 수정: InterviewRuntimeState
public class InterviewRuntimeState {
    private final Map<Long, TurnAnalysis> turnAnalysisCache = new ConcurrentHashMap<>();
    private volatile CachedResumeSkeleton resumeSkeletonCache;
    // ...
}
```

**대안(기각)**: 제네릭 `InterviewRuntimeState<T, R>`. plan 간 공유 상태가 generic parameter 로 고정되면 Store 단일 인스턴스에서 plan 별 분리 불가.

## 결과

- 신규: `backend/src/main/java/com/rehearse/api/domain/interview/runtime/TurnAnalysis.java`
- 신규: `backend/src/main/java/com/rehearse/api/domain/interview/runtime/CachedResumeSkeleton.java`
- 수정: `InterviewRuntimeState.java` — 필드 타입 교체
- 후속 plan-05 는 `ResumeSkeletonDto implements CachedResumeSkeleton` 형태로 도입
- 후속 plan-08 은 `RubricAnalysisResult implements TurnAnalysis` 형태로 도입
