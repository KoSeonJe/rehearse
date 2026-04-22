---
id: arch-00c-M1-store-srp-violation
severity: major
category: architecture
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# `InterviewRuntimeStateStore` SRP 위반 (캐시 + 메트릭 + 생성)

## 문제

Store 가 Caffeine 인스턴스 생성 + Micrometer 메트릭 등록 + 실제 캐시 오퍼레이션 세 책임을 동시에 가진다.

## 원인

`InterviewRuntimeStateStore.java:52-61` — 생성자/`@PostConstruct` 에서 `Caffeine.newBuilder()...build()` 와 `meterRegistry.gauge(...)` 를 직접 호출한다. 그 결과:

- `cache` 필드가 `@PostConstruct` 에서 초기화되어 `final` 선언 불가
- 테스트에서 `store.init()` 을 수동 호출해야 함 (`InterviewRuntimeStateStoreTest.java:25`) → 비정상 신호
- Caffeine 설정 변경을 Store 내부에서 해야 함 (다른 Config 파일과 불일치)

## 발생 상황

- **언제**: 캐시 TTL / 사이즈 조정 요구 발생 시, 또는 메트릭 태그 변경 시
- **누가**: Store 테스트 작성자 / 튜닝 담당자
- **파장**: 직접 장애는 없으나 변경 파장이 Store 로 집중 → 테스트 복잡도 증가 + 유지보수성 저하

## 해결 방법

Caffeine 인스턴스 생성 + 메트릭 바인딩을 별도 `@Configuration` 으로 분리. Store 는 생성자 주입으로 `Cache` 만 받음.

```java
// 신규: config/RuntimeCacheConfig.java
@Configuration
public class RuntimeCacheConfig {

    @Bean
    public Cache<Long, InterviewRuntimeState> interviewRuntimeStateCache(MeterRegistry registry) {
        Cache<Long, InterviewRuntimeState> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(2))
            .maximumSize(10_000)
            .recordStats()
            .build();
        CaffeineCacheMetrics.monitor(registry, cache, "rehearse.runtime.state");  // M2 해결도 함께
        return cache;
    }
}

// Store
@Component
public class InterviewRuntimeStateStore {
    private final Cache<Long, InterviewRuntimeState> cache;       // final
    private final InterviewLockService lockService;

    public InterviewRuntimeStateStore(Cache<Long, InterviewRuntimeState> cache,
                                       InterviewLockService lockService) {
        this.cache = cache;
        this.lockService = lockService;
    }
    // ...
}
```

## 결과

- 신규: `backend/src/main/java/com/rehearse/api/config/RuntimeCacheConfig.java`
- 수정: `InterviewRuntimeStateStore.java` — 생성자 주입, `@PostConstruct` 제거, `final` 필드화
- 수정: `InterviewRuntimeStateStoreTest.java` — `store.init()` 호출 삭제, `@SpringBootTest` 또는 직접 `Cache` 생성 후 주입
- 후속 plan 이 Store 를 주입받을 때 caching 설정을 알 필요 없음
