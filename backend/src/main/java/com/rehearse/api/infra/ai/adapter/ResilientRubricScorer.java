package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
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
public class ResilientRubricScorer implements RubricScorer {

    private static final String CALL_TYPE = "rubric_scoring";

    @Nullable
    private final OpenAiRubricScorer openAi;

    @Nullable
    private final ClaudeRubricScorer claude;

    private final AiCallMetrics aiCallMetrics;

    public ResilientRubricScorer(
            @Nullable OpenAiRubricScorer openAi,
            @Nullable ClaudeRubricScorer claude,
            AiCallMetrics aiCallMetrics) {
        if (openAi == null && claude == null) {
            throw new IllegalStateException("OpenAi/Claude RubricScorer 중 하나 이상 필요");
        }
        this.openAi = openAi;
        this.claude = claude;
        this.aiCallMetrics = aiCallMetrics;
    }

    @Override
    public RubricScoringResult score(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel level,
            Long interviewId,
            Long questionId
    ) {
        if (openAi == null) {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", false,
                    () -> claude.score(question, userAnswer, analysis, rubric,
                            dimensionsToScore, level, interviewId, questionId));
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "openai", false,
                    () -> openAi.score(question, userAnswer, analysis, rubric,
                            dimensionsToScore, level, interviewId, questionId));
        } catch (BusinessException e) {
            if (isNonRetryable(e)) {
                throw e;
            }
            log.warn("[RubricScorer Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(question, userAnswer, analysis, rubric,
                    dimensionsToScore, level, interviewId, questionId);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[RubricScorer Fallback] OpenAI 실패 → Claude 전환: {}", e.getMessage());
            return fallback(question, userAnswer, analysis, rubric,
                    dimensionsToScore, level, interviewId, questionId);
        }
    }

    private RubricScoringResult fallback(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel level,
            Long interviewId,
            Long questionId
    ) {
        if (claude == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return aiCallMetrics.recordCall(CALL_TYPE, "claude", true,
                    () -> claude.score(question, userAnswer, analysis, rubric,
                            dimensionsToScore, level, interviewId, questionId));
        } catch (Exception fallbackEx) {
            log.error("[RubricScorer Fallback] Claude 도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private boolean isNonRetryable(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
