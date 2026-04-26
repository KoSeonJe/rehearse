package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fallback 전략:
 * <ul>
 *   <li>OpenAI/Claude 중 한쪽만 설정 → 해당 provider 만 사용</li>
 *   <li>둘 다 설정 → OpenAI primary → 실패 시 Claude fallback (cache allowMiss=true)</li>
 *   <li>모두 실패 → SERVICE_UNAVAILABLE (503)</li>
 * </ul>
 */
@Slf4j
@Component
@Primary
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty() or !'${claude.api-key:}'.isEmpty()")
public class ResilientAiClient extends AbstractAiClient {

    @Nullable
    private final OpenAiClient openAiClient;

    @Nullable
    private final ClaudeApiClient claudeApiClient;

    private final AiCallMetrics aiCallMetrics;

    public ResilientAiClient(
            @Nullable OpenAiClient openAiClient,
            @Nullable ClaudeApiClient claudeApiClient,
            @Nullable SttService sttService,
            AiCallMetrics aiCallMetrics,
            QuestionGenerationAdapter questionAdapter,
            FollowUpGenerationAdapter followUpAdapter) {
        super(questionAdapter, followUpAdapter, sttService);
        this.openAiClient = openAiClient;
        this.claudeApiClient = claudeApiClient;
        this.aiCallMetrics = aiCallMetrics;

        if (openAiClient == null && claudeApiClient == null) {
            throw new IllegalStateException("OpenAiClient와 ClaudeApiClient 중 하나 이상 설정되어야 합니다.");
        }

        if (openAiClient != null && claudeApiClient != null) {
            log.info("[ResilientAiClient] Primary: OpenAI, Fallback: Claude");
        } else if (openAiClient != null) {
            log.info("[ResilientAiClient] OpenAI only (fallback 없음)");
        } else {
            log.info("[ResilientAiClient] Claude only (OpenAI 미설정)");
        }
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return aiCallMetrics.recordChat(request.callType(), () -> doChat(request));
    }

    @Override
    public ChatResponse chatWithAudio(ChatRequest request, MultipartFile audio) {
        return aiCallMetrics.recordChat(request.callType(), () -> doChatWithAudio(request, audio));
    }

    // Audio chat 은 OpenAI 만 지원. retryable 인프라 오류 발생 시
    // AudioChatFallbackRequiredException 으로 변환해 caller 가 STT + text-only 경로로 전환하도록 신호한다.
    // non-retryable 오류(CLIENT_ERROR / PARSE_FAILED)는 그대로 BusinessException 재던짐.
    private ChatResponse doChatWithAudio(ChatRequest request, MultipartFile audio) {
        if (openAiClient == null) {
            throw new AudioChatFallbackRequiredException("OpenAI 미설정 — audio chat 불가");
        }
        try {
            return openAiClient.chatWithAudio(request, audio);
        } catch (BusinessException e) {
            if (isNonRetryableError(e)) {
                throw e;
            }
            log.warn("[AI Audio] OpenAI audio chat 실패 → fallback 신호: callType={}, {}", request.callType(), e.getMessage());
            throw new AudioChatFallbackRequiredException("audio chat 인프라 오류: " + e.getMessage(), e);
        } catch (RestClientException | RetryableApiException e) {
            log.warn("[AI Audio] OpenAI audio chat 실패 → fallback 신호: callType={}, {}", request.callType(), e.getMessage());
            throw new AudioChatFallbackRequiredException("audio chat 네트워크 오류: " + e.getMessage(), e);
        }
    }

    private ChatResponse doChat(ChatRequest request) {
        if (openAiClient == null) {
            return fallbackChat(request);
        }

        try {
            return openAiClient.chat(request);
        } catch (BusinessException e) {
            if (isNonRetryableError(e)) {
                throw e;
            }
            log.warn("[AI Fallback] OpenAI chat 실패 → Claude 전환: callType={}, {}", request.callType(), e.getMessage());
            return fallbackChat(request);
        } catch (RestClientException | RetryableApiException e) {
            // 네트워크/API 오류만 fallback. 프로그래밍 오류(NPE, IAE 등)는 rethrow 하여 즉시 드러냄.
            log.warn("[AI Fallback] OpenAI chat 실패 → Claude 전환: callType={}, {}", request.callType(), e.getMessage());
            return fallbackChat(request);
        }
    }

    private ChatResponse fallbackChat(ChatRequest request) {
        if (claudeApiClient == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            ChatRequest fallbackReq = request.withCachePolicy(
                    request.cachePolicy().withAllowMiss(true)
            );
            ChatResponse response = claudeApiClient.chat(fallbackReq);
            return new ChatResponse(
                    response.content(),
                    response.usage(),
                    response.provider(),
                    response.model(),
                    response.cacheHit(),
                    true
            );
        } catch (Exception fallbackEx) {
            log.error("[AI Fallback] Claude chat도 실패 — 이중 장애: callType={}, {}", request.callType(), fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    // 요청 자체 문제(CLIENT_ERROR / PARSE_FAILED)는 Claude 로 보내도 동일 실패 → fallback 생략.
    private boolean isNonRetryableError(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
