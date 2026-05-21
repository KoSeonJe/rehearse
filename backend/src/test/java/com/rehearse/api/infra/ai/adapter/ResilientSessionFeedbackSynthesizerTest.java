package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import com.rehearse.api.domain.feedback.session.vo.StrengthItem;
import com.rehearse.api.domain.feedback.session.vo.GapItem;
import com.rehearse.api.domain.feedback.session.vo.WeekPlanItem;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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

@DisplayName("ResilientSessionFeedbackSynthesizer — OpenAI primary → Claude fallback 정책")
class ResilientSessionFeedbackSynthesizerTest {

    private OpenAiSessionFeedbackSynthesizer openAi;
    private ClaudeSessionFeedbackSynthesizer claude;
    private AiCallMetrics metrics;
    private ResilientSessionFeedbackSynthesizer resilient;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiSessionFeedbackSynthesizer.class);
        claude = mock(ClaudeSessionFeedbackSynthesizer.class);
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilient = new ResilientSessionFeedbackSynthesizer(openAi, claude, metrics);
    }

    private GeneratedSessionFeedback sample(String narrative) {
        return new GeneratedSessionFeedback(
                new OverallSection(Map.of("문제 정의", 2.5), "주니어", narrative, "all turns scored"),
                List.of(new StrengthItem("문제 정의", "obs", "matters")),
                List.of(new GapItem("기술 깊이", "obs", "gap", "action")),
                null,
                List.of(new WeekPlanItem(1, "topic", List.of("r"), "p"))
        );
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void synthesize_openAiSuccess_claudeNotCalled() {
        when(openAi.synthesize(any())).thenReturn(sample("openai-ok"));

        GeneratedSessionFeedback result = resilient.synthesize(sampleInput());

        assertThat(result.overall().narrative()).isEqualTo("openai-ok");
        verify(claude, never()).synthesize(any());
    }

    @Test
    @DisplayName("OpenAI RetryableApiException 발생 시 Claude fallback")
    void synthesize_openAiRetryable_claudeFallback() {
        when(openAi.synthesize(any())).thenThrow(new RetryableApiException("타임아웃"));
        when(claude.synthesize(any())).thenReturn(sample("claude-ok"));

        GeneratedSessionFeedback result = resilient.synthesize(sampleInput());

        assertThat(result.overall().narrative()).isEqualTo("claude-ok");
        verify(claude, times(1)).synthesize(any());
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void synthesize_clientError_noFallback() {
        when(openAi.synthesize(any()))
                .thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilient.synthesize(sampleInput()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);

        verify(claude, never()).synthesize(any());
    }

    @Test
    @DisplayName("AiErrorCode.PARSE_FAILED (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void synthesize_parseFailed_noFallback() {
        when(openAi.synthesize(any()))
                .thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilient.synthesize(sampleInput()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(claude, never()).synthesize(any());
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE")
    void synthesize_bothFail_serviceUnavailable() {
        when(openAi.synthesize(any())).thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claude.synthesize(any())).thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilient.synthesize(sampleInput()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OpenAI 미설정 (null) + Claude 존재 시 Claude 직접 호출")
    void synthesize_openAiNull_directlyCallClaude() {
        ResilientSessionFeedbackSynthesizer claudeOnly =
                new ResilientSessionFeedbackSynthesizer(null, claude, metrics);
        when(claude.synthesize(any())).thenReturn(sample("claude-only"));

        GeneratedSessionFeedback result = claudeOnly.synthesize(sampleInput());

        assertThat(result.overall().narrative()).isEqualTo("claude-only");
        verify(claude, times(1)).synthesize(any());
    }

    @Test
    @DisplayName("두 provider 모두 null 이면 생성자에서 IllegalStateException")
    void constructor_bothNull_throws() {
        assertThatThrownBy(() -> new ResilientSessionFeedbackSynthesizer(null, null, metrics))
                .isInstanceOf(IllegalStateException.class);
    }

    private SessionFeedbackInput sampleInput() {
        return new SessionFeedbackInput(
                new SessionFeedbackInput.SessionMetadata(
                        1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 2, 30),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList(),
                null, null, null, null,
                "all turns scored",
                InterviewLevel.MID
        );
    }
}
