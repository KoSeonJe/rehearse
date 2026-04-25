package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.Nullable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ResilientAiClient.chat() — Fallback 캐시 정책 degrade 검증.
 *
 * <p>검증 항목:
 * <ul>
 *   <li>OpenAI 성공 시 Claude 호출 없음</li>
 *   <li>OpenAI 실패 시 Claude fallback 호출 + allowMiss=true 적용</li>
 *   <li>fallback ChatResponse.fallbackUsed = true</li>
 *   <li>OpenAI + Claude 모두 실패 시 SERVICE_UNAVAILABLE</li>
 *   <li>CLIENT_ERROR (non-retryable) 는 fallback 시도 없이 즉시 throw</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ResilientAiClientFallbackTest {

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private ClaudeApiClient claudeApiClient;

    @Mock
    private SttService sttService;

    private ResilientAiClient resilientAiClient;

    private ChatRequest baseRequest;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        AiCallMetrics noopMetrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        resilientAiClient = new ResilientAiClient(
                openAiClient, claudeApiClient, sttService, noopMetrics,
                mock(QuestionGenerationAdapter.class), mock(FollowUpGenerationAdapter.class));

        baseRequest = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.of(ChatMessage.Role.SYSTEM, "시스템 프롬프트"),
                        ChatMessage.of(ChatMessage.Role.USER, "사용자 질문")
                ))
                .callType("test_fallback")
                .build();
    }

    @Test
    @DisplayName("OpenAI 성공 시 Claude 호출 없음")
    void chat_openAiSuccess_claudeNotCalled() {
        ChatResponse openAiResponse = new ChatResponse("OpenAI 응답", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        when(openAiClient.chat(any())).thenReturn(openAiResponse);

        ChatResponse result = resilientAiClient.chat(baseRequest);

        assertThat(result.provider()).isEqualTo("openai");
        assertThat(result.fallbackUsed()).isFalse();
        verify(claudeApiClient, never()).chat(any());
    }

    @Test
    @DisplayName("OpenAI 실패 시 Claude fallback 호출됨")
    void chat_openAiFails_claudeFallbackCalled() {
        when(openAiClient.chat(any())).thenThrow(new RetryableApiException("OpenAI 연결 실패"));

        ChatResponse claudeResponse = new ChatResponse("Claude 응답", ChatResponse.Usage.empty(), "claude", "claude-sonnet-4-20250514", false, false);
        when(claudeApiClient.chat(any())).thenReturn(claudeResponse);

        ChatResponse result = resilientAiClient.chat(baseRequest);

        assertThat(result.fallbackUsed()).isTrue();
        verify(claudeApiClient, times(1)).chat(any());
    }

    @Test
    @DisplayName("Fallback 경로에서 cachePolicy.allowMiss=true 로 degrade")
    void chat_fallback_allowMissSetToTrue() {
        when(openAiClient.chat(any())).thenThrow(new RetryableApiException("OpenAI 타임아웃"));

        ChatResponse claudeResponse = new ChatResponse("Claude 응답", ChatResponse.Usage.empty(), "claude", "claude-sonnet-4-20250514", false, false);
        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        when(claudeApiClient.chat(captor.capture())).thenReturn(claudeResponse);

        resilientAiClient.chat(baseRequest);

        ChatRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.cachePolicy().allowMiss()).isTrue();
    }

    @Test
    @DisplayName("원본 요청의 cachePolicy.allowMiss 는 변경되지 않음 (불변성)")
    void chat_fallback_originalRequestNotMutated() {
        when(openAiClient.chat(any())).thenThrow(new RetryableApiException("실패"));
        when(claudeApiClient.chat(any())).thenReturn(
                new ChatResponse("응답", ChatResponse.Usage.empty(), "claude", "model", false, false));

        resilientAiClient.chat(baseRequest);

        assertThat(baseRequest.cachePolicy().allowMiss()).isFalse();
    }

    @Test
    @DisplayName("ChatResponse.fallbackUsed = true — Claude fallback 사용 시")
    void chat_fallbackUsed_trueWhenClaudeUsed() {
        when(openAiClient.chat(any())).thenThrow(new RetryableApiException("실패"));
        when(claudeApiClient.chat(any())).thenReturn(
                new ChatResponse("Claude 응답", ChatResponse.Usage.empty(), "claude", "claude-sonnet-4-20250514", false, false));

        ChatResponse result = resilientAiClient.chat(baseRequest);

        assertThat(result.fallbackUsed()).isTrue();
    }

    @Test
    @DisplayName("OpenAI + Claude 모두 실패 → SERVICE_UNAVAILABLE")
    void chat_bothFail_serviceUnavailable() {
        when(openAiClient.chat(any())).thenThrow(new RetryableApiException("OpenAI 실패"));
        when(claudeApiClient.chat(any())).thenThrow(new RetryableApiException("Claude 실패"));

        assertThatThrownBy(() -> resilientAiClient.chat(baseRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE.getCode()));
    }

    @Test
    @DisplayName("CLIENT_ERROR (non-retryable) — fallback 시도 없이 즉시 throw")
    void chat_clientError_nonRetryable_noFallback() {
        when(openAiClient.chat(any())).thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> resilientAiClient.chat(baseRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.CLIENT_ERROR.getCode()));

        verify(claudeApiClient, never()).chat(any());
    }

    @Test
    @DisplayName("PARSE_FAILED (non-retryable) — fallback 시도 없이 즉시 throw")
    void chat_parseFailed_nonRetryable_noFallback() {
        when(openAiClient.chat(any())).thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> resilientAiClient.chat(baseRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));

        verify(claudeApiClient, never()).chat(any());
    }
}
