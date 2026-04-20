package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
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
 * Primary AiClient — OpenAI(GPT-4o-mini) 호출 후 실패 시 Claude fallback.
 *
 * <p>서비스 계층은 AiClient 인터페이스만 의존하므로 변경 없음.</p>
 *
 * <p>빈 생성 조건: OpenAiClient 또는 ClaudeApiClient 중 하나 이상 존재.
 * 둘 다 없으면 MockAiClient가 활성화됨.</p>
 *
 * <p>Fallback 전략:
 * <ul>
 *   <li>OpenAI만 있음: OpenAI 호출, 실패 시 에러</li>
 *   <li>Claude만 있음: Claude 호출</li>
 *   <li>둘 다 있음: OpenAI 호출 → 실패 시 Claude fallback</li>
 *   <li>Claude도 실패 → SERVICE_UNAVAILABLE (503)</li>
 * </ul>
 *
 * <p>legacy 3개 메서드(generateQuestions, generateFollowUpQuestion, generateFollowUpWithAudio)는
 * {@link AbstractAiClient} 를 통해 {@code chat()} 경유 어댑터로 위임된다.</p>
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
            // M4: 네트워크/API 레벨 오류만 fallback. 프로그래밍 오류(NPE, IAE 등)는 rethrow.
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

    /**
     * 요청 자체의 문제(CLIENT_ERROR, PARSE_FAILED)는 Claude로 보내도 동일하게 실패하므로 fallback하지 않는다.
     */
    private boolean isNonRetryableError(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }
}
