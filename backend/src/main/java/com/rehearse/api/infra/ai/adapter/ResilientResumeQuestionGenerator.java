package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
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
public class ResilientResumeQuestionGenerator implements ResumeQuestionGenerator {

    private static final String CALL_TYPE = "resume_question_generator";

    @Nullable
    private final OpenAiResumeQuestionGenerator openAi;

    @Nullable
    private final ClaudeResumeQuestionGenerator claude;

    private final AiCallMetrics aiCallMetrics;

    public ResilientResumeQuestionGenerator(
            @Nullable OpenAiResumeQuestionGenerator openAi,
            @Nullable ClaudeResumeQuestionGenerator claude,
            AiCallMetrics aiCallMetrics) {
        if (openAi == null && claude == null) {
            throw new IllegalStateException("OpenAi/Claude ResumeQuestionGenerator 중 하나 이상 필요");
        }
        this.openAi = openAi;
        this.claude = claude;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public GeneratedResumeQuestions generate(ResumeSkeleton skeleton, int openerCount, int mainCount) {
        if (openAi == null) {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", false,
                    () -> claude.generate(skeleton, openerCount, mainCount));
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "openai", false,
                    () -> openAi.generate(skeleton, openerCount, mainCount));
        } catch (BusinessException e) {
            if (isNonRetryable(e)) {
                throw e;
            }
            log.warn("[ResumeQuestionGenerator Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(skeleton, openerCount, mainCount);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[ResumeQuestionGenerator Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(skeleton, openerCount, mainCount);
        }
    }

    private GeneratedResumeQuestions fallback(ResumeSkeleton skeleton, int openerCount, int mainCount) {
        if (claude == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", true,
                    () -> claude.generate(skeleton, openerCount, mainCount));
        } catch (Exception fallbackEx) {
            log.error("[ResumeQuestionGenerator Fallback] Claude 도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isNonRetryable(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
