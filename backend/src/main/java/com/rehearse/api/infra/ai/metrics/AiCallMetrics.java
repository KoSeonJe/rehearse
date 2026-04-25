package com.rehearse.api.infra.ai.metrics;

import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
@RequiredArgsConstructor
public class AiCallMetrics {

    public static final String TIMER_NAME = "rehearse.ai.call.duration";
    public static final String TOKENS_INPUT = "rehearse.ai.call.tokens.input";
    public static final String TOKENS_OUTPUT = "rehearse.ai.call.tokens.output";
    public static final String TOKENS_CACHED_READ = "rehearse.ai.call.tokens.cached.read";
    public static final String TOKENS_CACHED_WRITE = "rehearse.ai.call.tokens.cached.write";

    private final MeterRegistry meterRegistry;
    private final ContextEngineeringMetrics contextMetrics;

    public ChatResponse recordChat(String callType, Callable<ChatResponse> callable) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        String provider = "unknown";
        String model = "unknown";
        String cacheHit = "unknown";
        String fallback = "false";
        ChatResponse response = null;

        try {
            response = callable.call();
            provider = response.provider();
            model = response.model();
            cacheHit = String.valueOf(response.cacheHit());
            fallback = String.valueOf(response.fallbackUsed());
            return response;
        } catch (Exception e) {
            outcome = "failure";
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        } finally {
            sample.stop(Timer.builder(TIMER_NAME)
                    .tag("call.type", callType)
                    .tag("model", model)
                    .tag("provider", provider)
                    .tag("cache.hit", cacheHit)
                    .tag("fallback", fallback)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
            if (response != null && response.usage() != null) {
                recordTokenUsage(callType, provider, model, response.usage());
            }
        }
    }

    private void recordTokenUsage(String callType, String provider, String model, ChatResponse.Usage usage) {
        incrementCounter(TOKENS_INPUT, callType, provider, model, usage.inputTokens());
        incrementCounter(TOKENS_OUTPUT, callType, provider, model, usage.outputTokens());
        incrementCounter(TOKENS_CACHED_READ, callType, provider, model, usage.cacheReadTokens());
        incrementCounter(TOKENS_CACHED_WRITE, callType, provider, model, usage.cacheWriteTokens());
        contextMetrics.recordCacheHit(provider, usage.cacheReadTokens(), usage.cacheWriteTokens());
    }

    private void incrementCounter(String name, String callType, String provider, String model, int amount) {
        if (amount <= 0) {
            return;
        }
        meterRegistry.counter(name,
                "call.type", callType,
                "provider", provider,
                "model", model).increment(amount);
    }
}
