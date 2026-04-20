package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AiCallMetrics — Micrometer Timer 태그 기록 검증.
 *
 * <p>plan 검증 #4 실측: call.type / model / provider / cache.hit / fallback / outcome 태그가
 * SimpleMeterRegistry 에 올바르게 기록되는지 확인한다.</p>
 */
@DisplayName("AiCallMetrics — Timer 태그 기록 검증")
class AiCallMetricsTest {

    private SimpleMeterRegistry registry;
    private AiCallMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(registry);
    }

    private ChatResponse successResponse(String provider, String model, boolean cacheHit, boolean fallback) {
        return new ChatResponse("content", ChatResponse.Usage.empty(), provider, model, cacheHit, fallback);
    }

    @Test
    @DisplayName("성공 호출 — Timer 가 success outcome 으로 기록된다")
    void recordChat_success_timerRegistered() {
        metrics.recordChat("generate_questions",
                () -> successResponse("openai", "gpt-4o-mini", false, false));

        Timer timer = registry.find(AiCallMetrics.TIMER_NAME)
                .tag("outcome", "success")
                .tag("call.type", "generate_questions")
                .tag("provider", "openai")
                .tag("model", "gpt-4o-mini")
                .tag("cache.hit", "false")
                .tag("fallback", "false")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("fallback=true — fallback 태그가 'true' 로 기록된다")
    void recordChat_fallback_taggedTrue() {
        metrics.recordChat("generate_questions",
                () -> successResponse("claude", "claude-sonnet-4-20250514", false, true));

        Timer timer = registry.find(AiCallMetrics.TIMER_NAME)
                .tag("fallback", "true")
                .tag("provider", "claude")
                .timer();

        assertThat(timer).isNotNull();
    }

    @Test
    @DisplayName("cache.hit=true — cache.hit 태그가 'true' 로 기록된다")
    void recordChat_cacheHit_taggedTrue() {
        metrics.recordChat("intent_classifier",
                () -> successResponse("claude", "claude-sonnet-4-20250514", true, false));

        Timer timer = registry.find(AiCallMetrics.TIMER_NAME)
                .tag("cache.hit", "true")
                .timer();

        assertThat(timer).isNotNull();
    }

    @Test
    @DisplayName("예외 발생 — outcome=failure 로 기록되고 예외가 rethrow 된다")
    void recordChat_exception_outcomeFailure_andRethrows() {
        BusinessException cause = new BusinessException(AiErrorCode.TIMEOUT);

        assertThatThrownBy(() ->
                metrics.recordChat("generate_questions", () -> { throw cause; })
        ).isInstanceOf(BusinessException.class);

        Timer timer = registry.find(AiCallMetrics.TIMER_NAME)
                .tag("outcome", "failure")
                .tag("call.type", "generate_questions")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("provider/model 미확정(예외) — unknown 태그로 기록된다")
    void recordChat_exceptionBeforeResponse_unknownTags() {
        assertThatThrownBy(() ->
                metrics.recordChat("test_type",
                        () -> { throw new RuntimeException("network error"); })
        ).isInstanceOf(RuntimeException.class);

        Timer timer = registry.find(AiCallMetrics.TIMER_NAME)
                .tag("provider", "unknown")
                .tag("model", "unknown")
                .tag("outcome", "failure")
                .timer();

        assertThat(timer).isNotNull();
    }

    @Test
    @DisplayName("복수 호출 — 동일 태그 조합이면 count 누적된다")
    void recordChat_multipleCallsSameTags_countAccumulates() {
        Callable<ChatResponse> call = () -> successResponse("openai", "gpt-4o-mini", false, false);

        metrics.recordChat("generate_questions", call);
        metrics.recordChat("generate_questions", call);
        metrics.recordChat("generate_questions", call);

        Timer timer = registry.find(AiCallMetrics.TIMER_NAME)
                .tag("call.type", "generate_questions")
                .tag("outcome", "success")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(3);
    }
}
