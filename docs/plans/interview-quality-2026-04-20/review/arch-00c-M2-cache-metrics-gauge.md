---
id: arch-00c-M2-cache-metrics-gauge
severity: major
category: architecture
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# Caffeine 메트릭이 gauge 로 등록 — counter 의미 상실

## 문제

`rehearse.runtime.state.cache.{hits,misses,evictions}` 은 단조증가 카운터인데 `MeterRegistry.gauge()` 로 등록됨. Prometheus `rate()` / `increase()` 쿼리에서 인스턴스 재시작(리셋) 감지 불가 → 운영 모니터링 정확도 저하.

## 원인

`InterviewRuntimeStateStore.java:55-60` — `meterRegistry.gauge("rehearse.runtime.state.cache.hits", cache, c -> c.stats().hitCount())` 형태로 등록. Micrometer 는 Caffeine 전용 표준 바인더 `CaffeineCacheMetrics` 를 이미 제공하는데 사용하지 않음.

## 발생 상황

- **언제**: Prometheus 대시보드/알람에서 캐시 hit ratio 또는 eviction rate 모니터링 시
- **누가**: 운영/SRE 팀
- **파장**: counter 특성 미반영 → gauge rate 계산이 리셋 시점에 음수로 튀거나 평탄화됨. 실제 트렌드 왜곡

## 해결 방법

`CaffeineCacheMetrics.monitor()` 한 줄로 교체. M1 의 `RuntimeCacheConfig` 에 포함되므로 Store 에서 메트릭 코드 전부 제거.

```java
// Before (Store 내부)
meterRegistry.gauge("rehearse.runtime.state.cache.hits", cache, c -> c.stats().hitCount());
meterRegistry.gauge("rehearse.runtime.state.cache.misses", cache, c -> c.stats().missCount());
meterRegistry.gauge("rehearse.runtime.state.cache.evictions", cache, c -> c.stats().evictionCount());

// After (RuntimeCacheConfig)
CaffeineCacheMetrics.monitor(registry, cache, "rehearse.runtime.state");
```

`CaffeineCacheMetrics` 는 `cache.gets{result="hit|miss"}` / `cache.evictions` 등을 **counter** 로 등록한다 (Micrometer 표준 네이밍).

## 결과

- 수정: `backend/src/main/java/com/rehearse/api/config/RuntimeCacheConfig.java` (M1 과 통합)
- 제거: `InterviewRuntimeStateStore` 의 수동 gauge 등록 3줄
- 메트릭 네이밍 변경: `rehearse.runtime.state.cache.hits` → `cache.gets{name="rehearse.runtime.state", result="hit"}`
- Grafana 대시보드가 있다면 네이밍 변경 반영 필요 (현재 없음으로 운영 영향 0)
- 의존성: `io.micrometer:micrometer-core` 에 `CaffeineCacheMetrics` 포함되어 있는지 확인 (Spring Boot 3.x 기본 포함)
