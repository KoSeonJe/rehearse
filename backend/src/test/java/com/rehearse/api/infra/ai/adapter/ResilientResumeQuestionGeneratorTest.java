package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions.GeneratedResumeQuestion;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResilientResumeQuestionGenerator — OpenAI primary → Claude fallback 정책")
class ResilientResumeQuestionGeneratorTest {

    private OpenAiResumeQuestionGenerator openAi;
    private ClaudeResumeQuestionGenerator claude;
    private AiCallMetrics metrics;
    private ResilientResumeQuestionGenerator resilient;
    private ResumeSkeleton skeleton;

    @BeforeEach
    void setUp() {
        openAi = mock(OpenAiResumeQuestionGenerator.class);
        claude = mock(ClaudeResumeQuestionGenerator.class);
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        metrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilient = new ResilientResumeQuestionGenerator(openAi, claude, metrics);

        skeleton = new ResumeSkeleton(
                "r-1", "hash", CandidateLevel.MID, "backend",
                List.of(new Project("p1", "주문 캐싱", List.of("Redis"), "백엔드", "Cache-Aside", List.of("TTL"))));
    }

    private GeneratedResumeQuestions sample(String tag) {
        return new GeneratedResumeQuestions(
                List.of(new GeneratedResumeQuestion("opener-" + tag, "tts", "best")),
                List.of(new GeneratedResumeQuestion("main-" + tag, "tts", "best")));
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void generate_openAiSuccess_claudeNotCalled() {
        when(openAi.generate(any(), anyInt(), anyInt())).thenReturn(sample("openai"));

        GeneratedResumeQuestions result = resilient.generate(skeleton, 1, 7);

        assertThat(result.openers().get(0).question()).isEqualTo("opener-openai");
        verify(claude, never()).generate(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("OpenAI RetryableApiException 발생 시 Claude fallback")
    void generate_openAiRetryable_claudeFallback() {
        when(openAi.generate(any(), anyInt(), anyInt())).thenThrow(new RetryableApiException("타임아웃"));
        when(claude.generate(any(), anyInt(), anyInt())).thenReturn(sample("claude"));

        GeneratedResumeQuestions result = resilient.generate(skeleton, 1, 7);

        assertThat(result.openers().get(0).question()).isEqualTo("opener-claude");
        verify(claude, times(1)).generate(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void generate_clientError_noFallback() {
        when(openAi.generate(any(), anyInt(), anyInt()))
                .thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilient.generate(skeleton, 1, 7))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.CLIENT_ERROR);

        verify(claude, never()).generate(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("PARSE_FAILED (non-retryable) 는 fallback 시도 없이 즉시 throw")
    void generate_parseFailed_noFallback() {
        when(openAi.generate(any(), anyInt(), anyInt()))
                .thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilient.generate(skeleton, 1, 7))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);

        verify(claude, never()).generate(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE")
    void generate_bothFail_serviceUnavailable() {
        when(openAi.generate(any(), anyInt(), anyInt())).thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claude.generate(any(), anyInt(), anyInt())).thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilient.generate(skeleton, 1, 7))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("OpenAI 미설정 (null) + Claude 존재 시 Claude 직접 호출")
    void generate_openAiNull_directlyCallClaude() {
        ResilientResumeQuestionGenerator claudeOnly = new ResilientResumeQuestionGenerator(null, claude, metrics);
        when(claude.generate(any(), anyInt(), anyInt())).thenReturn(sample("claude"));

        GeneratedResumeQuestions result = claudeOnly.generate(skeleton, 1, 7);

        assertThat(result.openers().get(0).question()).isEqualTo("opener-claude");
        verify(claude, times(1)).generate(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("두 provider 모두 null 이면 생성자에서 IllegalStateException")
    void constructor_bothNull_throws() {
        assertThatThrownBy(() -> new ResilientResumeQuestionGenerator(null, null, metrics))
                .isInstanceOf(IllegalStateException.class);
    }
}
