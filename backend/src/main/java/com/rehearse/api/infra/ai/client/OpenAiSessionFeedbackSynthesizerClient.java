package com.rehearse.api.infra.ai.client;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.properties.OpenAiSessionFeedbackSynthesizerProperties;
import com.rehearse.api.infra.ai.dto.openai.OpenAiResponse;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiSessionFeedbackSynthesizerClient {

    private static final String CALL_TYPE = "session_feedback_synthesizer";

    private final RestClient restClient;
    private final OpenAiSessionFeedbackSynthesizerProperties properties;
    private final String apiKey;

    public OpenAiSessionFeedbackSynthesizerClient(
            @Qualifier("openAiSessionFeedbackSynthesizerRestClient") RestClient openAiSessionFeedbackSynthesizerRestClient,
            OpenAiSessionFeedbackSynthesizerProperties properties,
            OpenAiCommonProperties commonProperties) {
        this.restClient = openAiSessionFeedbackSynthesizerRestClient;
        this.properties = properties;
        this.apiKey = commonProperties.apiKey();
    }

    @RateLimiter(name = "openai-api")
    @Retryable(
            retryFor = {RetryableApiException.class, RestClientException.class},
            noRetryFor = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, random = true)
    )
    public String call(String systemPrompt, String userPrompt) {
        Map<String, Object> body = buildRequestBody(systemPrompt, userPrompt);
        return execute(body);
    }

    @Recover
    public String recoverCall(Exception e, String systemPrompt, String userPrompt) {
        log.error("[OpenAI Session Feedback Synthesizer] 재시도 최종 실패 callType={}", CALL_TYPE, e);
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("max_completion_tokens", properties.maxTokens());
        body.put("temperature", properties.temperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private String execute(Map<String, Object> requestBody) {
        String apiLabel = "OpenAI API [" + CALL_TYPE + "]";
        OpenAiResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, (req, res) -> {
                    log.warn("[{}] Rate Limited (429)", apiLabel);
                    throw new RetryableApiException(apiLabel + " rate limited (429)");
                })
                .onStatus(status -> status.is4xxClientError() && status.value() != 429, (req, res) -> {
                    String errBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "(empty body)";
                    log.error("[{}] 클라이언트 에러: status={}, body={}", apiLabel, res.getStatusCode(), errBody);
                    throw new BusinessException(AiErrorCode.CLIENT_ERROR);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    log.warn("[{}] 서버 에러: status={}", apiLabel, res.getStatusCode());
                    throw new RetryableApiException(apiLabel + " 서버 에러: " + res.getStatusCode());
                })
                .body(OpenAiResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }

        if (response.getUsage() != null) {
            var usage = response.getUsage();
            log.info("[{}] 토큰 사용량 prompt={}, completion={}, total={}",
                    apiLabel, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }

        OpenAiResponse.Choice choice = response.getChoices().get(0);
        String content = choice.getMessage() != null ? choice.getMessage().getContent() : null;
        if (content == null || content.isBlank()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }
        return content;
    }
}
