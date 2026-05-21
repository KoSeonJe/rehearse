package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.question.models.service.StandardQuestionGenerator;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * OpenAI primary → Claude fallback. 두 provider 중 한쪽만 설정되어도 활성화된다.
 */
@Slf4j
@Component
@Primary
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty() or !'${claude.api-key:}'.isEmpty()")
public class ResilientStandardQuestionGenerator implements StandardQuestionGenerator {

    private static final String CALL_TYPE = "generate_questions";

    @Nullable
    private final OpenAiStandardQuestionGenerator openAi;

    @Nullable
    private final ClaudeStandardQuestionGenerator claude;

    private final AiCallMetrics aiCallMetrics;

    public ResilientStandardQuestionGenerator(
            @Nullable OpenAiStandardQuestionGenerator openAi,
            @Nullable ClaudeStandardQuestionGenerator claude,
            AiCallMetrics aiCallMetrics) {
        if (openAi == null && claude == null) {
            throw new IllegalStateException("OpenAi/Claude StandardQuestionGenerator 중 하나 이상 필요");
        }
        this.openAi = openAi;
        this.claude = claude;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public List<GeneratedQuestion> generate(QuestionGenerationRequest request) {
        if (openAi == null) {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", false, () -> claude.generate(request));
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "openai", false, () -> openAi.generate(request));
        } catch (BusinessException e) {
            if (isNonRetryable(e)) {
                throw e;
            }
            log.warn("[StandardQuestionGenerator Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(request);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[StandardQuestionGenerator Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(request);
        }
    }

    private List<GeneratedQuestion> fallback(QuestionGenerationRequest request) {
        if (claude == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", true, () -> claude.generate(request));
        } catch (Exception fallbackEx) {
            log.error("[StandardQuestionGenerator Fallback] Claude 도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isNonRetryable(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
