package com.rehearse.api.domain.question.service;

import java.util.List;
import java.util.Set;

public record PoolSelectionCriteria(
        String cacheKey,
        int requiredCount,
        List<String> categoryFilter,
        Set<Long> usedPoolIds
) {

    public static PoolSelectionCriteria of(String cacheKey, int requiredCount) {
        return new PoolSelectionCriteria(cacheKey, requiredCount, null, null);
    }

    public static PoolSelectionCriteria of(String cacheKey, int requiredCount, List<String> categoryFilter) {
        return new PoolSelectionCriteria(cacheKey, requiredCount, categoryFilter, null);
    }

    public boolean hasCategoryFilter() {
        return categoryFilter != null && !categoryFilter.isEmpty();
    }

    public boolean hasUsedPoolIds() {
        return usedPoolIds != null && !usedPoolIds.isEmpty();
    }
}
