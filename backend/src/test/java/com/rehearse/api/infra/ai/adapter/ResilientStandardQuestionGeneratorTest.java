package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResilientStandardQuestionGenerator — OpenAI primary → Claude fallback 정책")
class ResilientStandardQuestionGeneratorTest {

    private OpenAiStandardQuestionGenerator openAi;
    private ClaudeStandardQuestionGenerator claude;
    private AiCallMetrics metrics;
    private ResilientStandardQuestionGenerator resilient;
    private QuestionGenerationRequest request;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiStandardQuestionGenerator.class);
        claude = mock(ClaudeStandardQuestionGenerator.class);
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilient = new ResilientStandardQuestionGenerator(openAi, claude, metrics);

        request = new QuestionGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL), Set.of(), null, null);
    }

    private GeneratedQuestion sampleQuestion(String content) {
        return new GeneratedQuestion(content, "tts", "JVM", 1, "criteria", "CS", "best", "strategy");
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void generate_openAiSuccess_claudeNotCalled() {
        when(openAi.generate(any())).thenReturn(List.of(sampleQuestion("q1")));

        List<GeneratedQuestion> result = resilient.generate(request);

        assertThat(result).hasSize(1);
        verify(claude, never()).generate(any());
    }

    @Test
    @DisplayName("OpenAI RetryableApiException 발생 시 Claude fallback 호출")
    void generate_openAiRetryable_claudeFallback() {
        when(openAi.generate(any())).thenThrow(new RetryableApiException("타임아웃"));
        when(claude.generate(any())).thenReturn(List.of(sampleQuestion("c1")));

        List<GeneratedQuestion> result = resilient.generate(request);

        assertThat(result).hasSize(1);
        verify(claude, times(1)).generate(any());
    }

    @Test
    @DisplayName("OpenAI 5xx / RestClientException (Retryable) 시 Claude fallback")
    void generate_openAi5xx_claudeFallback() {
        when(openAi.generate(any())).thenThrow(new org.springframework.web.client.RestClientException("5xx"));
        when(claude.generate(any())).thenReturn(List.of(sampleQuestion("c1")));

        List<GeneratedQuestion> result = resilient.generate(request);

        assertThat(result).hasSize(1);
        verify(claude, times(1)).generate(any());
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void generate_clientError_noFallback() {
        when(openAi.generate(any())).thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilient.generate(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);

        verify(claude, never()).generate(any());
    }

    @Test
    @DisplayName("PARSE_FAILED (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void generate_parseFailed_noFallback() {
        when(openAi.generate(any())).thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilient.generate(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(claude, never()).generate(any());
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE")
    void generate_bothFail_serviceUnavailable() {
        when(openAi.generate(any())).thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claude.generate(any())).thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilient.generate(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OpenAI 미설정 (null) + Claude 존재 시 Claude 직접 호출")
    void generate_openAiNull_directlyCallClaude() {
        ResilientStandardQuestionGenerator claudeOnly = new ResilientStandardQuestionGenerator(null, claude, metrics);
        when(claude.generate(any())).thenReturn(List.of(sampleQuestion("c1")));

        List<GeneratedQuestion> result = claudeOnly.generate(request);

        assertThat(result).hasSize(1);
        verify(claude, times(1)).generate(any());
    }

    @Test
    @DisplayName("두 provider 모두 null 이면 생성자에서 IllegalStateException")
    void constructor_bothNull_throws() {
        assertThatThrownBy(() -> new ResilientStandardQuestionGenerator(null, null, metrics))
                .isInstanceOf(IllegalStateException.class);
    }
}
