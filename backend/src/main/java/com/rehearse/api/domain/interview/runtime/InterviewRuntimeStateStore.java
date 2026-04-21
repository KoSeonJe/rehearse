package com.rehearse.api.domain.interview.runtime;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class InterviewRuntimeStateStore {

    private static final int MAX_SIZE = 10_000;
    private static final Duration IDLE_TTL = Duration.ofHours(2);
    private static final String METRIC_PREFIX = "rehearse.runtime.state.cache";

    private final MeterRegistry meterRegistry;

    private Cache<Long, InterviewRuntimeState> cache;

    @PostConstruct
    void init() {
        cache = Caffeine.newBuilder()
                .expireAfterAccess(IDLE_TTL)
                .maximumSize(MAX_SIZE)
                .recordStats()
                .build();

        registerMetrics();
    }

    public InterviewRuntimeState getOrInit(Long interviewId, Supplier<InterviewRuntimeState> init) {
        return cache.get(interviewId, id -> init.get());
    }

    public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) {
        InterviewRuntimeState state = cache.get(interviewId, id -> new InterviewRuntimeState());
        mutator.accept(state);
    }

    public void evict(Long interviewId) {
        cache.invalidate(interviewId);
    }

    private void registerMetrics() {
        Tags tags = Tags.empty();

        meterRegistry.gauge(METRIC_PREFIX + ".hits", tags, cache,
                c -> c.stats().hitCount());
        meterRegistry.gauge(METRIC_PREFIX + ".misses", tags, cache,
                c -> c.stats().missCount());
        meterRegistry.gauge(METRIC_PREFIX + ".evictions", tags, cache,
                c -> c.stats().evictionCount());
    }

    CacheStats stats() {
        return cache.stats();
    }
}
