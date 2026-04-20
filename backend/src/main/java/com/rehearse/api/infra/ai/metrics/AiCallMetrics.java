package com.rehearse.api.infra.ai.metrics;

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

    private final MeterRegistry meterRegistry;

    public ChatResponse recordChat(String callType, Callable<ChatResponse> callable) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        String provider = "unknown";
        String model = "unknown";
        String cacheHit = "unknown";
        String fallback = "false";

        try {
            ChatResponse response = callable.call();
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
        }
    }
}
