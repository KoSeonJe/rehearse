package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResilientAnswerAnalyzer — OpenAI primary → Claude fallback 정책")
class ResilientAnswerAnalyzerTest {

    private OpenAiAnswerAnalyzer openAi;
    private ClaudeAnswerAnalyzer claude;
    private AiCallMetrics metrics;
    private ResilientAnswerAnalyzer resilient;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiAnswerAnalyzer.class);
        claude = mock(ClaudeAnswerAnalyzer.class);
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilient = new ResilientAnswerAnalyzer(openAi, claude, metrics);
    }

    private GeneratedAnswerAnalysis sample(String dim) {
        return new GeneratedAnswerAnalysis(
                "전사 텍스트",
                List.of(),
                Map.of("clarity", 2, "depth", 1),
                dim,
                List.of(),
                RecommendedNextAction.DEEP_DIVE);
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void analyze_openAiSuccess_claudeNotCalled() {
        when(openAi.analyze(anyLong(), any(), any(), any(), anyBoolean())).thenReturn(sample("depth"));

        GeneratedAnswerAnalysis result = resilient.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false);

        assertThat(result.weakestDimension()).isEqualTo("depth");
        verify(claude, never()).analyze(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("OpenAI RetryableApiException 발생 시 Claude fallback")
    void analyze_openAiRetryable_claudeFallback() {
        when(openAi.analyze(anyLong(), any(), any(), any(), anyBoolean())).thenThrow(new RetryableApiException("타임아웃"));
        when(claude.analyze(anyLong(), any(), any(), any(), anyBoolean())).thenReturn(sample("evidence"));

        GeneratedAnswerAnalysis result = resilient.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false);

        assertThat(result.weakestDimension()).isEqualTo("evidence");
        verify(claude, times(1)).analyze(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void analyze_clientError_noFallback() {
        when(openAi.analyze(anyLong(), any(), any(), any(), anyBoolean()))
                .thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilient.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);

        verify(claude, never()).analyze(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("PARSE_FAILED (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void analyze_parseFailed_noFallback() {
        when(openAi.analyze(anyLong(), any(), any(), any(), anyBoolean()))
                .thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilient.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(claude, never()).analyze(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE")
    void analyze_bothFail_serviceUnavailable() {
        when(openAi.analyze(anyLong(), any(), any(), any(), anyBoolean())).thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claude.analyze(anyLong(), any(), any(), any(), anyBoolean())).thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilient.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OpenAI 미설정 (null) + Claude 존재 시 Claude 직접 호출")
    void analyze_openAiNull_directlyCallClaude() {
        ResilientAnswerAnalyzer claudeOnly = new ResilientAnswerAnalyzer(null, claude, metrics);
        when(claude.analyze(anyLong(), any(), any(), any(), anyBoolean())).thenReturn(sample("clarity"));

        GeneratedAnswerAnalysis result = claudeOnly.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false);

        assertThat(result.weakestDimension()).isEqualTo("clarity");
        verify(claude, times(1)).analyze(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("두 provider 모두 null 이면 생성자에서 IllegalStateException")
    void constructor_bothNull_throws() {
        assertThatThrownBy(() -> new ResilientAnswerAnalyzer(null, null, metrics))
                .isInstanceOf(IllegalStateException.class);
    }
}
