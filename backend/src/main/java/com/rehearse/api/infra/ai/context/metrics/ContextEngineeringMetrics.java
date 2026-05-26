package com.rehearse.api.infra.ai.context.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ContextEngineeringMetrics {

    static final String CACHE_HIT_RATIO_METRIC = "rehearse.ai.context.cache_hit_ratio";

    private final MeterRegistry registry;

    // Per-provider rolling counters backing the cache_hit_ratio gauge.
    // reads = cacheReadTokens calls, writes = cacheWriteTokens calls (proxy for cache writes).
    private final ConcurrentHashMap<String, AtomicLong> cacheReads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> cacheWrites = new ConcurrentHashMap<>();

    // Tracks which providers already have a registered gauge to avoid duplicate registration.
    private final Map<String, Boolean> gaugeRegistered = new ConcurrentHashMap<>();

    public ContextEngineeringMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCacheHit(String provider, int cacheReadTokens, int cacheWriteTokens) {
        AtomicLong reads = cacheReads.computeIfAbsent(provider, k -> new AtomicLong(0));
        AtomicLong writes = cacheWrites.computeIfAbsent(provider, k -> new AtomicLong(0));

        if (cacheReadTokens > 0) {
            reads.addAndGet(cacheReadTokens);
        }
        if (cacheWriteTokens > 0) {
            writes.addAndGet(cacheWriteTokens);
        }

        gaugeRegistered.computeIfAbsent(provider, p -> {
            Gauge.builder(CACHE_HIT_RATIO_METRIC, reads, r -> computeRatio(r, writes))
                    .tag("provider", p)
                    .register(registry);
            return Boolean.TRUE;
        });
    }

    private double computeRatio(AtomicLong reads, AtomicLong writes) {
        long r = reads.get();
        long w = writes.get();
        long total = r + w;
        return total == 0 ? 0.0 : (double) r / total;
    }
}
