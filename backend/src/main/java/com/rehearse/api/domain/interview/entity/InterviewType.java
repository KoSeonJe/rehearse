package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.questionpool.entity.CacheStrategy;

import static com.rehearse.api.domain.questionpool.entity.CacheStrategy.*;

public enum InterviewType {
    // 공통
    CS_FUNDAMENTAL(CACHEABLE),
    BEHAVIORAL(CACHEABLE),
    RESUME_BASED(FRESH),

    // 언어/프레임워크 심화
    LANGUAGE_FRAMEWORK(CACHEABLE),
    SYSTEM_DESIGN(CACHEABLE),

    // 풀스택 기술 심화
    FULLSTACK_STACK(CACHEABLE),

    // UI 프레임워크 심화
    UI_FRAMEWORK(CACHEABLE),
    BROWSER_PERFORMANCE(CACHEABLE),

    // 데브옵스 특화
    INFRA_CICD(CACHEABLE),
    CLOUD(CACHEABLE),

    // 데이터 특화
    DATA_PIPELINE(CACHEABLE),
    SQL_MODELING(CACHEABLE);

    private final CacheStrategy cacheStrategy;

    InterviewType(CacheStrategy cacheStrategy) {
        this.cacheStrategy = cacheStrategy;
    }

    public CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    public boolean isCacheable() {
        return cacheStrategy == CACHEABLE;
    }
}
