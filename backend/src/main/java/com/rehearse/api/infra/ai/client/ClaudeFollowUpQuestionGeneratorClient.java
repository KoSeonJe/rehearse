package com.rehearse.api.infra.ai.client;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.config.ClaudeFollowUpQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.dto.claude.ClaudeRequest;
import com.rehearse.api.infra.ai.dto.claude.ClaudeResponse;
import com.rehearse.api.infra.ai.dto.claude.SystemContent;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeFollowUpQuestionGeneratorClient {

    private static final String CALL_TYPE = "follow_up_generator_v3";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ClaudeFollowUpQuestionGeneratorProperties properties;
    private final String apiKey;

    public ClaudeFollowUpQuestionGeneratorClient(
            @Qualifier("claudeFollowUpQuestionGeneratorRestClient") RestClient claudeFollowUpQuestionGeneratorRestClient,
            ClaudeFollowUpQuestionGeneratorProperties properties,
            @Value("${claude.api-key}") String apiKey) {
        this.restClient = claudeFollowUpQuestionGeneratorRestClient;
        this.properties = properties;
        this.apiKey = apiKey;
    }

    @RateLimiter(name = "claude-api")
    @Retryable(
            retryFor = {RetryableApiException.class, RestClientException.class},
            noRetryFor = {BusinessException.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, random = true)
    )
    public String call(String systemPrompt, String userPrompt) {
        ClaudeRequest request = ClaudeRequest.builder()
                .model(properties.model())
                .maxTokens(properties.maxTokens())
                .system(List.of(SystemContent.withCaching(systemPrompt)))
                .messages(List.of(
                        ClaudeRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .temperature(properties.temperature())
                .build();
        return execute(request);
    }

    @Recover
    public String recoverCall(Exception e, String systemPrompt, String userPrompt) {
        log.error("[Claude FollowUp Question Generator] 재시도 최종 실패 callType={}", CALL_TYPE, e);
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }

    private String execute(ClaudeRequest request) {
        String apiLabel = "Claude API [" + CALL_TYPE + "]";
        ClaudeResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .body(request)
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
                .body(ClaudeResponse.class);

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }

        if (response.getUsage() != null) {
            var usage = response.getUsage();
            log.info("[{}] 토큰 사용량 input={}, output={}, cache_write={}, cache_read={}",
                    apiLabel, usage.getInputTokens(), usage.getOutputTokens(),
                    usage.getCacheCreationInputTokens(), usage.getCacheReadInputTokens());
        }

        String content = response.getContent().get(0).getText();
        if (content == null || content.isBlank()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }
        return content;
    }
}
