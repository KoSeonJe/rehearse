package com.rehearse.api.domain.questionpool.entity;

public enum CacheStrategy {
    CACHEABLE,  // 이력서/경험에 의존하지 않음 → DB pool에서 제공 가능
    FRESH       // 이력서/경험에 의존 → 항상 Claude 실시간 생성
}
