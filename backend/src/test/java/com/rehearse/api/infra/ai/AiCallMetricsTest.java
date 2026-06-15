package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.Counter;
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
        metrics = new AiCallMetrics(registry, new ContextEngineeringMetrics(registry));
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

    private ChatResponse responseWithUsage(ChatResponse.Usage usage) {
        return new ChatResponse("content", usage, "openai", "gpt-4o-mini", false, false);
    }

    @Test
    @DisplayName("토큰 사용량 — input/output Counter 가 Usage 값만큼 증가한다")
    void recordChat_tokenCounters_incrementByUsage() {
        ChatResponse.Usage usage = ChatResponse.Usage.of(1200, 340);

        metrics.recordChat("generate_questions", () -> responseWithUsage(usage));

        Counter input = registry.find(AiCallMetrics.TOKENS_INPUT)
                .tag("call.type", "generate_questions")
                .tag("provider", "openai")
                .tag("model", "gpt-4o-mini")
                .counter();
        Counter output = registry.find(AiCallMetrics.TOKENS_OUTPUT)
                .tag("call.type", "generate_questions")
                .tag("provider", "openai")
                .tag("model", "gpt-4o-mini")
                .counter();

        assertThat(input).isNotNull();
        assertThat(input.count()).isEqualTo(1200.0);
        assertThat(output).isNotNull();
        assertThat(output.count()).isEqualTo(340.0);
    }

    @Test
    @DisplayName("캐시 read/write 토큰 — 각각 별도 Counter 에 누적된다")
    void recordChat_cachedTokens_splitReadWrite() {
        ChatResponse.Usage usage = ChatResponse.Usage.of(800, 200, 500, 300);

        metrics.recordChat("intent_classifier", () -> responseWithUsage(usage));

        Counter cachedRead = registry.find(AiCallMetrics.TOKENS_CACHED_READ)
                .tag("call.type", "intent_classifier")
                .counter();
        Counter cachedWrite = registry.find(AiCallMetrics.TOKENS_CACHED_WRITE)
                .tag("call.type", "intent_classifier")
                .counter();

        assertThat(cachedRead).isNotNull();
        assertThat(cachedRead.count()).isEqualTo(500.0);
        assertThat(cachedWrite).isNotNull();
        assertThat(cachedWrite.count()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("0 토큰 — Counter 가 등록되지 않는다 (무의미 시리즈 방지)")
    void recordChat_zeroTokens_counterNotRegistered() {
        metrics.recordChat("generate_questions",
                () -> responseWithUsage(ChatResponse.Usage.empty()));

        assertThat(registry.find(AiCallMetrics.TOKENS_INPUT).counter()).isNull();
        assertThat(registry.find(AiCallMetrics.TOKENS_OUTPUT).counter()).isNull();
        assertThat(registry.find(AiCallMetrics.TOKENS_CACHED_READ).counter()).isNull();
        assertThat(registry.find(AiCallMetrics.TOKENS_CACHED_WRITE).counter()).isNull();
    }

    @Test
    @DisplayName("예외 경로 — 토큰 Counter 가 기록되지 않는다")
    void recordChat_exception_noTokenCounter() {
        assertThatThrownBy(() ->
                metrics.recordChat("generate_questions",
                        () -> { throw new BusinessException(AiErrorCode.TIMEOUT); })
        ).isInstanceOf(BusinessException.class);

        assertThat(registry.find(AiCallMetrics.TOKENS_INPUT).counter()).isNull();
        assertThat(registry.find(AiCallMetrics.TOKENS_OUTPUT).counter()).isNull();
    }

    @Test
    @DisplayName("복수 호출 — 동일 태그 조합 토큰 Counter 가 누적된다")
    void recordChat_multipleCalls_tokenCountersAccumulate() {
        Callable<ChatResponse> call = () -> responseWithUsage(ChatResponse.Usage.of(100, 50));

        metrics.recordChat("generate_questions", call);
        metrics.recordChat("generate_questions", call);

        Counter input = registry.find(AiCallMetrics.TOKENS_INPUT)
                .tag("call.type", "generate_questions")
                .counter();

        assertThat(input).isNotNull();
        assertThat(input.count()).isEqualTo(200.0);
    }
}
