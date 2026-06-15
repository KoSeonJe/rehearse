package com.rehearse.api.infra.ai.context.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ContextEngineeringMetrics — cache_hit_ratio gauge 검증")
class ContextEngineeringMetricsTest {

    private SimpleMeterRegistry registry;
    private ContextEngineeringMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ContextEngineeringMetrics(registry);
    }

    @Test
    @DisplayName("cache_hit_ratio — 데이터 없을 때 0.0 을 반환한다")
    void cache_hit_ratio_gauge_returns_zero_when_no_data() {
        metrics.recordCacheHit("openai", 0, 0);

        Gauge gauge = registry.find(ContextEngineeringMetrics.CACHE_HIT_RATIO_METRIC)
                .tag("provider", "openai")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("cache_hit_ratio — read 3회 write 1회 기록 시 ratio 0.75 를 반환한다")
    void cache_hit_ratio_gauge_reflects_running_ratio() {
        metrics.recordCacheHit("openai", 1000, 0);
        metrics.recordCacheHit("openai", 1000, 0);
        metrics.recordCacheHit("openai", 1000, 0);
        metrics.recordCacheHit("openai", 0, 1000);

        Gauge gauge = registry.find(ContextEngineeringMetrics.CACHE_HIT_RATIO_METRIC)
                .tag("provider", "openai")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isCloseTo(0.75, within(0.001));
    }

    @Test
    @DisplayName("cache_hit_ratio — provider 별로 격리되어 서로 오염되지 않는다")
    void cache_ratio_isolated_per_provider() {
        // openai: read=3000, write=1000 → ratio=0.75
        metrics.recordCacheHit("openai", 3000, 1000);

        // claude: read=1000, write=1000 → ratio=0.5
        metrics.recordCacheHit("claude", 1000, 1000);

        Gauge openaiGauge = registry.find(ContextEngineeringMetrics.CACHE_HIT_RATIO_METRIC)
                .tag("provider", "openai")
                .gauge();
        Gauge claudeGauge = registry.find(ContextEngineeringMetrics.CACHE_HIT_RATIO_METRIC)
                .tag("provider", "claude")
                .gauge();

        assertThat(openaiGauge).isNotNull();
        assertThat(openaiGauge.value()).isCloseTo(0.75, within(0.001));

        assertThat(claudeGauge).isNotNull();
        assertThat(claudeGauge.value()).isCloseTo(0.5, within(0.001));
    }
}
