package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResilientFollowUpQuestionGenerator — OpenAI primary → Claude fallback 정책")
class ResilientFollowUpQuestionGeneratorTest {

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);

    private OpenAiFollowUpQuestionGenerator openAi;
    private ClaudeFollowUpQuestionGenerator claude;
    private AiCallMetrics metrics;
    private ResilientFollowUpQuestionGenerator resilient;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiFollowUpQuestionGenerator.class);
        claude = mock(ClaudeFollowUpQuestionGenerator.class);
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilient = new ResilientFollowUpQuestionGenerator(openAi, claude, metrics);
    }

    private GeneratedFollowUp sample(String reason) {
        return new GeneratedFollowUp(
                false, null, "질문 본문", "질문 본문", reason, "DEEP_DIVE", null, "A", null);
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void generate_openAiSuccess_claudeNotCalled() {
        when(openAi.generate(any(), any(), any())).thenReturn(sample("openai-ok"));

        GeneratedFollowUp result = resilient.generate("Q", "A", SAMPLE_ANALYSIS);

        assertThat(result.reason()).isEqualTo("openai-ok");
        verify(claude, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("OpenAI RetryableApiException 발생 시 Claude fallback")
    void generate_openAiRetryable_claudeFallback() {
        when(openAi.generate(any(), any(), any())).thenThrow(new RetryableApiException("타임아웃"));
        when(claude.generate(any(), any(), any())).thenReturn(sample("claude-ok"));

        GeneratedFollowUp result = resilient.generate("Q", "A", SAMPLE_ANALYSIS);

        assertThat(result.reason()).isEqualTo("claude-ok");
        verify(claude, times(1)).generate(any(), any(), any());
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void generate_clientError_noFallback() {
        when(openAi.generate(any(), any(), any()))
                .thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilient.generate("Q", "A", SAMPLE_ANALYSIS))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);

        verify(claude, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("PARSE_FAILED (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void generate_parseFailed_noFallback() {
        when(openAi.generate(any(), any(), any()))
                .thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilient.generate("Q", "A", SAMPLE_ANALYSIS))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(claude, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE")
    void generate_bothFail_serviceUnavailable() {
        when(openAi.generate(any(), any(), any())).thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claude.generate(any(), any(), any())).thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilient.generate("Q", "A", SAMPLE_ANALYSIS))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OpenAI 미설정 (null) + Claude 존재 시 Claude 직접 호출")
    void generate_openAiNull_directlyCallClaude() {
        ResilientFollowUpQuestionGenerator claudeOnly = new ResilientFollowUpQuestionGenerator(null, claude, metrics);
        when(claude.generate(any(), any(), any())).thenReturn(sample("claude-only"));

        GeneratedFollowUp result = claudeOnly.generate("Q", "A", SAMPLE_ANALYSIS);

        assertThat(result.reason()).isEqualTo("claude-only");
        verify(claude, times(1)).generate(any(), any(), any());
    }

    @Test
    @DisplayName("두 provider 모두 null 이면 생성자에서 IllegalStateException")
    void constructor_bothNull_throws() {
        assertThatThrownBy(() -> new ResilientFollowUpQuestionGenerator(null, null, metrics))
                .isInstanceOf(IllegalStateException.class);
    }
}
