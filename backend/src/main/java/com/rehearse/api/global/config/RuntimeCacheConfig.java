package com.rehearse.api.global.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class RuntimeCacheConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public Cache<Long, InterviewRuntimeState> interviewRuntimeStateCache(MeterRegistry registry) {
        Cache<Long, InterviewRuntimeState> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(8))
                .maximumSize(10_000)
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(registry, cache, "rehearse.runtime.state");
        return cache;
    }
}
