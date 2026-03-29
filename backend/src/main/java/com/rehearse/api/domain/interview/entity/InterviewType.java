package com.rehearse.api.domain.interview.entity;

public enum InterviewType {
    // 공통
    CS_FUNDAMENTAL(CacheStrategy.CACHEABLE),
    BEHAVIORAL(CacheStrategy.CACHEABLE),
    RESUME_BASED(CacheStrategy.FRESH),

    // 언어/프레임워크 심화
    LANGUAGE_FRAMEWORK(CacheStrategy.CACHEABLE),
    SYSTEM_DESIGN(CacheStrategy.CACHEABLE),

    // 풀스택 기술 심화
    FULLSTACK_STACK(CacheStrategy.CACHEABLE),

    // UI 프레임워크 심화
    UI_FRAMEWORK(CacheStrategy.CACHEABLE),
    BROWSER_PERFORMANCE(CacheStrategy.CACHEABLE),

    // 데브옵스 특화
    INFRA_CICD(CacheStrategy.CACHEABLE),
    CLOUD(CacheStrategy.CACHEABLE),

    // 데이터 특화
    DATA_PIPELINE(CacheStrategy.CACHEABLE),
    SQL_MODELING(CacheStrategy.CACHEABLE);

    private final CacheStrategy cacheStrategy;

    InterviewType(CacheStrategy cacheStrategy) {
        this.cacheStrategy = cacheStrategy;
    }

    public CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    public boolean isCacheable() {
        return cacheStrategy == CacheStrategy.CACHEABLE;
    }

    /** 캐싱 가능 여부를 나타내는 전략 (InterviewType 전용) */
    public enum CacheStrategy {
        CACHEABLE,  // 이력서/경험에 의존하지 않음 → DB pool에서 제공 가능
        FRESH       // 이력서/경험에 의존 → 항상 Claude 실시간 생성
    }
}
