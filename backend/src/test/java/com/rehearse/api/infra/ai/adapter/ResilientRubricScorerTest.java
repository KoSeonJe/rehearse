package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResilientRubricScorer — OpenAI primary → Claude fallback 정책")
class ResilientRubricScorerTest {

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);

    private OpenAiRubricScorer openAi;
    private ClaudeRubricScorer claude;
    private AiCallMetrics metrics;
    private ResilientRubricScorer resilient;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiRubricScorer.class);
        claude = mock(ClaudeRubricScorer.class);
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilient = new ResilientRubricScorer(openAi, claude, metrics);
    }

    private RubricScoringResult sample(String rubricId) {
        return new RubricScoringResult(
                rubricId, List.of("technical_depth"),
                Map.of("technical_depth", new DimensionScore(2, "ok", "evidence", com.rehearse.api.domain.feedback.score.entity.DimensionStatus.OK)),
                null);
    }

    private Rubric rubric() {
        return new Rubric("test-v1", "테스트",
                List.of(new DimensionRef("technical_depth", 1.0)),
                Map.of(), Map.of());
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void score_openAiSuccess_claudeNotCalled() {
        when(openAi.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn(sample("openai-ok"));

        RubricScoringResult result = resilient.score(
                mock(Question.class), "A", SAMPLE_ANALYSIS, rubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L);

        assertThat(result.rubricId()).isEqualTo("openai-ok");
        verify(claude, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("OpenAI RetryableApiException 발생 시 Claude fallback")
    void score_openAiRetryable_claudeFallback() {
        when(openAi.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenThrow(new RetryableApiException("타임아웃"));
        when(claude.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn(sample("claude-ok"));

        RubricScoringResult result = resilient.score(
                mock(Question.class), "A", SAMPLE_ANALYSIS, rubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L);

        assertThat(result.rubricId()).isEqualTo("claude-ok");
        verify(claude, times(1)).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void score_clientError_noFallback() {
        when(openAi.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilient.score(
                mock(Question.class), "A", SAMPLE_ANALYSIS, rubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);

        verify(claude, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("PARSE_FAILED (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void score_parseFailed_noFallback() {
        when(openAi.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilient.score(
                mock(Question.class), "A", SAMPLE_ANALYSIS, rubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(claude, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE")
    void score_bothFail_serviceUnavailable() {
        when(openAi.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claude.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilient.score(
                mock(Question.class), "A", SAMPLE_ANALYSIS, rubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OpenAI 미설정 (null) + Claude 존재 시 Claude 직접 호출")
    void score_openAiNull_directlyCallClaude() {
        ResilientRubricScorer claudeOnly = new ResilientRubricScorer(null, claude, metrics);
        when(claude.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                .thenReturn(sample("claude-only"));

        RubricScoringResult result = claudeOnly.score(
                mock(Question.class), "A", SAMPLE_ANALYSIS, rubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L);

        assertThat(result.rubricId()).isEqualTo("claude-only");
        verify(claude, times(1)).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("두 provider 모두 null 이면 생성자에서 IllegalStateException")
    void constructor_bothNull_throws() {
        assertThatThrownBy(() -> new ResilientRubricScorer(null, null, metrics))
                .isInstanceOf(IllegalStateException.class);
    }
}
