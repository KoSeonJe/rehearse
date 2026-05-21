package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
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
public class ResilientAnswerAnalyzer implements AnswerAnalyzer {

    private static final String CALL_TYPE = "answer_analyzer";

    @Nullable
    private final OpenAiAnswerAnalyzer openAi;

    @Nullable
    private final ClaudeAnswerAnalyzer claude;

    private final AiCallMetrics aiCallMetrics;

    public ResilientAnswerAnalyzer(
            @Nullable OpenAiAnswerAnalyzer openAi,
            @Nullable ClaudeAnswerAnalyzer claude,
            AiCallMetrics aiCallMetrics) {
        if (openAi == null && claude == null) {
            throw new IllegalStateException("OpenAi/Claude AnswerAnalyzer 중 하나 이상 필요");
        }
        this.openAi = openAi;
        this.claude = claude;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public GeneratedAnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        if (openAi == null) {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", false,
                    () -> claude.analyze(interviewId, mainQuestion, questionReferenceType, userAnswer, isResumeTrack));
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "openai", false,
                    () -> openAi.analyze(interviewId, mainQuestion, questionReferenceType, userAnswer, isResumeTrack));
        } catch (BusinessException e) {
            if (isNonRetryable(e)) {
                throw e;
            }
            log.warn("[AnswerAnalyzer Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(interviewId, mainQuestion, questionReferenceType, userAnswer, isResumeTrack);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[AnswerAnalyzer Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(interviewId, mainQuestion, questionReferenceType, userAnswer, isResumeTrack);
        }
    }

    private GeneratedAnswerAnalysis fallback(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        if (claude == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", true,
                    () -> claude.analyze(interviewId, mainQuestion, questionReferenceType, userAnswer, isResumeTrack));
        } catch (Exception fallbackEx) {
            log.error("[AnswerAnalyzer Fallback] Claude 도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isNonRetryable(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
