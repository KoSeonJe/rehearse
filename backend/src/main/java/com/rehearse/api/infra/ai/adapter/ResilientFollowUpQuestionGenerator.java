package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.models.service.FollowUpQuestionGenerator;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
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
public class ResilientFollowUpQuestionGenerator implements FollowUpQuestionGenerator {

    private static final String CALL_TYPE = "follow_up_generator_v3";

    @Nullable
    private final OpenAiFollowUpQuestionGenerator openAi;

    @Nullable
    private final ClaudeFollowUpQuestionGenerator claude;

    private final AiCallMetrics aiCallMetrics;

    public ResilientFollowUpQuestionGenerator(
            @Nullable OpenAiFollowUpQuestionGenerator openAi,
            @Nullable ClaudeFollowUpQuestionGenerator claude,
            AiCallMetrics aiCallMetrics) {
        if (openAi == null && claude == null) {
            throw new IllegalStateException("OpenAi/Claude FollowUpQuestionGenerator 중 하나 이상 필요");
        }
        this.openAi = openAi;
        this.claude = claude;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public GeneratedFollowUp generate(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis,
            ResumeSkeleton resumeSkeleton
    ) {
        if (openAi == null) {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", false,
                    () -> claude.generate(mainQuestion, userAnswer, analysis, resumeSkeleton));
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "openai", false,
                    () -> openAi.generate(mainQuestion, userAnswer, analysis, resumeSkeleton));
        } catch (BusinessException e) {
            if (isNonRetryable(e)) {
                throw e;
            }
            log.warn("[FollowUpQuestionGenerator Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(mainQuestion, userAnswer, analysis, resumeSkeleton);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[FollowUpQuestionGenerator Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(mainQuestion, userAnswer, analysis, resumeSkeleton);
        }
    }

    private GeneratedFollowUp fallback(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis,
            ResumeSkeleton resumeSkeleton
    ) {
        if (claude == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", true,
                    () -> claude.generate(mainQuestion, userAnswer, analysis, resumeSkeleton));
        } catch (Exception fallbackEx) {
            log.error("[FollowUpQuestionGenerator Fallback] Claude 도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isNonRetryable(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
