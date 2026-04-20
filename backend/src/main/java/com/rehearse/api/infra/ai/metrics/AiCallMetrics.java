package com.rehearse.api.infra.ai.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * AI 호출 Micrometer 계측 컴포넌트.
 *
 * <p>타이머 이름: {@code rehearse.ai.call.duration} (단위: seconds)</p>
 *
 * <p>태그:
 * <ul>
 *   <li>{@code call.type} — ChatRequest.callType (예: "generate_questions", "intent_classifier")</li>
 *   <li>{@code model} — 실제 사용된 모델 ID (예: "gpt-4o-mini", "claude-sonnet-4-20250514")</li>
 *   <li>{@code provider} — "openai" / "claude" / "mock"</li>
 *   <li>{@code cache.hit} — "true" / "false" / "unknown" (호출 전에는 알 수 없으므로 사후 기록)</li>
 *   <li>{@code fallback} — "true" / "false" (ResilientAiClient 가 fallback 경로를 사용했는지)</li>
 *   <li>{@code outcome} — "success" / "failure"</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCallMetrics {

    public static final String TIMER_NAME = "rehearse.ai.call.duration";

    private final MeterRegistry meterRegistry;

    /**
     * AI 호출을 Timer 로 계측하며 실행한다. 결과에서 provider/model/cacheHit/fallbackUsed 를 추출해 태그로 기록한다.
     *
     * @param callType    ChatRequest.callType 값
     * @param callable    실제 AI 호출 로직 (ChatResponse 반환)
     * @return ChatResponse
     */
    public com.rehearse.api.infra.ai.dto.ChatResponse recordChat(
            String callType,
            Callable<com.rehearse.api.infra.ai.dto.ChatResponse> callable) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        String provider = "unknown";
        String model = "unknown";
        String cacheHit = "unknown";
        String fallback = "false";

        try {
            com.rehearse.api.infra.ai.dto.ChatResponse response = callable.call();
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
