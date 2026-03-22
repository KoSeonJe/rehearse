package com.rehearse.api.domain.questionpool.config;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.questionpool.entity.CacheStrategy;

import java.util.Map;

import static com.rehearse.api.domain.interview.entity.InterviewType.*;
import static com.rehearse.api.domain.questionpool.entity.CacheStrategy.*;

public final class CacheStrategyConfig {

    private static final Map<InterviewType, CacheStrategy> STRATEGY_MAP = Map.ofEntries(
            Map.entry(CS_FUNDAMENTAL, CACHEABLE),
            Map.entry(LANGUAGE_FRAMEWORK, CACHEABLE),
            Map.entry(SYSTEM_DESIGN, CACHEABLE),
            Map.entry(UI_FRAMEWORK, CACHEABLE),
            Map.entry(BROWSER_PERFORMANCE, CACHEABLE),
            Map.entry(INFRA_CICD, CACHEABLE),
            Map.entry(CLOUD, CACHEABLE),
            Map.entry(DATA_PIPELINE, CACHEABLE),
            Map.entry(SQL_MODELING, CACHEABLE),
            Map.entry(FULLSTACK_STACK, CACHEABLE),
            Map.entry(RESUME_BASED, FRESH),
            Map.entry(BEHAVIORAL, FRESH)
    );

    private CacheStrategyConfig() {
    }

    public static CacheStrategy getStrategy(InterviewType type) {
        return STRATEGY_MAP.getOrDefault(type, FRESH);
    }

    public static boolean isCacheable(InterviewType type) {
        return getStrategy(type) == CACHEABLE;
    }
}
