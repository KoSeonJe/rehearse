package com.rehearse.api.infra.ai.context.metrics;

import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ContextEngineeringMetrics — 3 meters 기록 검증")
class ContextEngineeringMetricsTest {

    private SimpleMeterRegistry registry;
    private ContextEngineeringMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new ContextEngineeringMetrics(registry);
    }

    private BuiltContext builtContextWith(Map<String, Integer> perLayerTokens) {
        return new BuiltContext(
                List.of(ChatMessage.of(ChatMessage.Role.USER, "test")),
                perLayerTokens.values().stream().mapToInt(Integer::intValue).sum(),
                perLayerTokens
        );
    }

    @Test
    @DisplayName("레이어별 토큰 분포 — L1/L2/L3/L4/total 5개 DistributionSummary 가 callType 태그와 함께 기록된다")
    void records_token_distribution_per_layer_and_call_type() {
        Map<String, Integer> perLayer = Map.of(
                "L1", 4000,
                "L2", 400,
                "L3", 2000,
                "L4", 600,
                "total", 7000
        );
        BuiltContext built = builtContextWith(perLayer);

        metrics.recordContextTokens("answer_analyzer", built);

        String[] layers = {"L1", "L2", "L3", "L4", "total"};
        for (String layer : layers) {
            DistributionSummary summary = registry.find(ContextEngineeringMetrics.TOKENS_METRIC)
                    .tag("layer", layer)
                    .tag("call.type", "answer_analyzer")
                    .summary();
            assertThat(summary).as("DistributionSummary for layer=%s", layer).isNotNull();
            assertThat(summary.count()).isEqualTo(1);
            assertThat(summary.totalAmount()).isEqualTo(perLayer.get(layer).doubleValue());
        }
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
    @DisplayName("compaction_count — async 모드 카운터가 증가한다")
    void compaction_count_increments_with_mode_tag() {
        metrics.recordCompaction("async");
        metrics.recordCompaction("async");
        metrics.recordCompaction("sync_fallback");

        Counter asyncCounter = registry.find(ContextEngineeringMetrics.COMPACTION_COUNT_METRIC)
                .tag("mode", "async")
                .counter();
        Counter syncCounter = registry.find(ContextEngineeringMetrics.COMPACTION_COUNT_METRIC)
                .tag("mode", "sync_fallback")
                .counter();

        assertThat(asyncCounter).isNotNull();
        assertThat(asyncCounter.count()).isEqualTo(2.0);
        assertThat(syncCounter).isNotNull();
        assertThat(syncCounter.count()).isEqualTo(1.0);
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
