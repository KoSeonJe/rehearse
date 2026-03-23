package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeApiClient implements AiClient {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final int MAX_TOKENS_QUESTION = 8192;
    private static final int MAX_TOKENS_FOLLOW_UP = 1024;
    private static final String FOLLOW_UP_MODEL = "claude-haiku-4-5-20251001";

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final RestClient restClient;
    private final QuestionGenerationPromptBuilder questionPromptBuilder;
    private final FollowUpPromptBuilder followUpPromptBuilder;
    private final ClaudeResponseParser responseParser;
    private final String apiKey;
    private final String model;

    public ClaudeApiClient(
            RestClient.Builder restClientBuilder,
            QuestionGenerationPromptBuilder questionPromptBuilder,
            FollowUpPromptBuilder followUpPromptBuilder,
            ClaudeResponseParser responseParser,
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.model:claude-sonnet-4-20250514}") String model) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl(ANTHROPIC_API_URL)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        this.questionPromptBuilder = questionPromptBuilder;
        this.followUpPromptBuilder = followUpPromptBuilder;
        this.responseParser = responseParser;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    @RateLimiter(name = "claude-api")
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        String systemPrompt = questionPromptBuilder.buildSystemPrompt(request);
        String userPrompt = questionPromptBuilder.buildUserPrompt(request);

        String text = callClaudeApi(systemPrompt, userPrompt, MAX_TOKENS_QUESTION, 0.9);
        GeneratedQuestionsWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedQuestionsWrapper.class);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        return wrapper.getQuestions();
    }

    @Override
    @RateLimiter(name = "claude-api")
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        String systemPrompt = followUpPromptBuilder.buildSystemPrompt(request);
        String userPrompt = followUpPromptBuilder.buildUserPrompt(request);

        String text = callClaudeApiWithModel(FOLLOW_UP_MODEL, systemPrompt, userPrompt, MAX_TOKENS_FOLLOW_UP, 1.0);
        return responseParser.parseJsonResponse(text, GeneratedFollowUp.class);
    }

    private String callClaudeApi(String systemPrompt, String userPrompt, int maxTokens, Double temperature) {
        return callClaudeApiWithModel(model, systemPrompt, userPrompt, maxTokens, temperature);
    }

    private String callClaudeApiWithModel(String requestModel, String systemPrompt, String userPrompt,
                                          int maxTokens, Double temperature) {
        SystemContent systemContent = SystemContent.withCaching(systemPrompt);

        ClaudeRequest request = ClaudeRequest.builder()
                .model(requestModel)
                .maxTokens(maxTokens)
                .system(List.of(systemContent))
                .messages(List.of(
                        ClaudeRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .temperature(temperature)
                .build();

        long delayMs = INITIAL_RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                ClaudeResponse response = restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", ANTHROPIC_VERSION)
                        .body(request)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            log.warn("[Claude API] Rate Limited (429)");
                            throw new RetryableApiException("Claude API rate limited (429)");
                        })
                        .onStatus(status -> status.is4xxClientError() && status.value() != 429, (req, res) -> {
                            String body = new String(res.getBody().readAllBytes());
                            log.error("Claude API 클라이언트 에러: status={}, body={}", res.getStatusCode(), body);
                            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                            log.warn("[Claude API] 서버 에러: status={}", res.getStatusCode());
                            throw new RetryableApiException("Claude API 서버 에러: " + res.getStatusCode());
                        })
                        .body(ClaudeResponse.class);

                if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                    throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
                }

                if (response.getUsage() != null) {
                    var usage = response.getUsage();
                    log.info("[Claude API] 토큰 사용량 - input: {}, output: {}, cache_write: {}, cache_read: {}, total: {}",
                            usage.getInputTokens(), usage.getOutputTokens(),
                            usage.getCacheCreationInputTokens(), usage.getCacheReadInputTokens(),
                            usage.getInputTokens() + usage.getOutputTokens()
                                    + usage.getCacheCreationInputTokens() + usage.getCacheReadInputTokens());
                }

                if ("max_tokens".equals(response.getStopReason())) {
                    log.warn("[Claude API] 응답이 max_tokens({})에 도달하여 잘림. model={}", maxTokens, requestModel);
                }

                return response.getContent().get(0).getText();

            } catch (BusinessException e) {
                throw e;
            } catch (RetryableApiException | RestClientException e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.info("[Claude API] 재시도 {}/{}: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(AiErrorCode.TIMEOUT);
                    }
                    delayMs *= 2;
                } else {
                    log.error("[Claude API] 모든 재시도 실패", e);
                    throw new BusinessException(AiErrorCode.TIMEOUT);
                }
            }
        }
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }
}
