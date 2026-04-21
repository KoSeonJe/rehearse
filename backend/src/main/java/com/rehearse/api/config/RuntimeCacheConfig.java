package com.rehearse.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rehearse.api.domain.interview.runtime.InterviewRuntimeState;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RuntimeCacheConfig {

    @Bean
    public Cache<Long, InterviewRuntimeState> interviewRuntimeStateCache(MeterRegistry registry) {
        Cache<Long, InterviewRuntimeState> cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(2))
                .maximumSize(10_000)
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(registry, cache, "rehearse.runtime.state");
        return cache;
    }
}
