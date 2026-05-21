package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.session.models.service.SessionFeedbackSynthesizer;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * OpenAI primary → Claude fallback. 두 provider 중 한쪽만 설정되어도 활성화된다.
 */
@Slf4j
@Component
@Primary
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty() or !'${claude.api-key:}'.isEmpty()")
public class ResilientSessionFeedbackSynthesizer implements SessionFeedbackSynthesizer {

    private static final String CALL_TYPE = "session_feedback_synthesizer";

    @Nullable
    private final OpenAiSessionFeedbackSynthesizer openAi;

    @Nullable
    private final ClaudeSessionFeedbackSynthesizer claude;

    private final AiCallMetrics aiCallMetrics;

    public ResilientSessionFeedbackSynthesizer(
            @Nullable OpenAiSessionFeedbackSynthesizer openAi,
            @Nullable ClaudeSessionFeedbackSynthesizer claude,
            AiCallMetrics aiCallMetrics) {
        if (openAi == null && claude == null) {
            throw new IllegalStateException("OpenAi/Claude SessionFeedbackSynthesizer 중 하나 이상 필요");
        }
        this.openAi = openAi;
        this.claude = claude;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public GeneratedSessionFeedback synthesize(SessionFeedbackInput input) {
        if (openAi == null) {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", false,
                    () -> claude.synthesize(input));
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "openai", false,
                    () -> openAi.synthesize(input));
        } catch (BusinessException e) {
            if (isNonRetryable(e)) {
                throw e;
            }
            log.warn("[SessionFeedbackSynthesizer Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(input);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[SessionFeedbackSynthesizer Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(input);
        }
    }

    private GeneratedSessionFeedback fallback(SessionFeedbackInput input) {
        if (claude == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", true,
                    () -> claude.synthesize(input));
        } catch (Exception fallbackEx) {
            log.error("[SessionFeedbackSynthesizer Fallback] Claude 도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isNonRetryable(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
