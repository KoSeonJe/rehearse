package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.dto.claude.CacheControl;
import com.rehearse.api.infra.ai.dto.claude.ClaudeRequest;
import com.rehearse.api.infra.ai.dto.claude.ClaudeResponse;
import com.rehearse.api.infra.ai.dto.claude.SystemContent;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;

import java.util.ArrayList;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
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
public class ClaudeApiClient {

    private static final String DEFAULT_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final int MAX_TOKENS_QUESTION = 8192;
    private static final int MAX_TOKENS_FOLLOW_UP = 1024;
    private static final double TEMPERATURE_FOLLOW_UP = 0.7;
    private static final String FOLLOW_UP_MODEL = "claude-haiku-4-5-20251001";

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final RestClient restClient;
    private final QuestionGenerationPromptBuilder questionPromptBuilder;
    private final FollowUpPromptBuilder followUpPromptBuilder;
    private final AiResponseParser responseParser;
    private final String apiKey;
    private final String model;

    public ClaudeApiClient(
            RestClient.Builder restClientBuilder,
            QuestionGenerationPromptBuilder questionPromptBuilder,
            FollowUpPromptBuilder followUpPromptBuilder,
            AiResponseParser responseParser,
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.model:claude-sonnet-4-20250514}") String model,
            @Value("${claude.api.url:https://api.anthropic.com/v1/messages}") String apiUrl) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl(apiUrl)
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
        this.questionPromptBuilder = questionPromptBuilder;
        this.followUpPromptBuilder = followUpPromptBuilder;
        this.responseParser = responseParser;
        this.apiKey = apiKey;
        this.model = model;
    }

    // Claude 는 response_format 파라미터 미지원 → system 메시지 앞에 prepend 해서 JSON 강제.
    private static final String JSON_OBJECT_INSTRUCTION =
            "You MUST respond with a single JSON object only. No prose, no markdown, no code fences.";

    @RateLimiter(name = "claude-api")
    public ChatResponse chat(ChatRequest req) {
        String resolvedModel = (req.modelOverride() != null && !req.modelOverride().isBlank())
                ? req.modelOverride()
                : model;

        int resolvedMaxTokens = (req.maxTokens() != null) ? req.maxTokens() : MAX_TOKENS_FOLLOW_UP;
        Double resolvedTemperature = req.temperature();

        List<SystemContent> systemContents = new ArrayList<>();
        List<ClaudeRequest.Message> userMessages = new ArrayList<>();

        if (req.responseFormat() == ResponseFormat.JSON_OBJECT) {
            systemContents.add(SystemContent.of(JSON_OBJECT_INSTRUCTION));
        }

        for (ChatMessage msg : req.messages()) {
            if (msg.role() == ChatMessage.Role.SYSTEM) {
                if (msg.cacheControl()) {
                    systemContents.add(SystemContent.withCaching(msg.content()));
                } else {
                    systemContents.add(SystemContent.of(msg.content()));
                }
            } else {
                userMessages.add(ClaudeRequest.Message.builder()
                        .role(msg.roleLowercase())
                        .content(msg.content())
                        .build());
            }
        }

        if (userMessages.isEmpty()) {
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }

        ClaudeRequest claudeRequest = ClaudeRequest.builder()
                .model(resolvedModel)
                .maxTokens(resolvedMaxTokens)
                .system(systemContents.isEmpty() ? null : systemContents)
                .messages(userMessages)
                .temperature(resolvedTemperature)
                .build();

        String apiLabel = "Claude API [" + req.callType() + "]";
        ClaudeResponse response = executeClaudeRequest(claudeRequest, apiLabel, resolvedMaxTokens);

        int cacheRead = 0;
        int cacheWrite = 0;
        int inputTokens = 0;
        int outputTokens = 0;
        boolean cacheHit = false;

        if (response.getUsage() != null) {
            var usage = response.getUsage();
            inputTokens = usage.getInputTokens();
            outputTokens = usage.getOutputTokens();
            cacheRead = usage.getCacheReadInputTokens();
            cacheWrite = usage.getCacheCreationInputTokens();
            cacheHit = cacheRead > 0;
        }

        String content = response.getContent().get(0).getText();
        if (content == null || content.isBlank()) {
            throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
        }

        return new ChatResponse(
                content,
                ChatResponse.Usage.of(inputTokens, outputTokens, cacheRead, cacheWrite),
                "claude",
                resolvedModel,
                cacheHit,
                false
        );
    }

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

    @RateLimiter(name = "claude-api")
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        String systemPrompt = followUpPromptBuilder.buildSystemPrompt(request);
        String userPrompt = followUpPromptBuilder.buildUserPrompt(request);

        String text = callClaudeApiWithModel(FOLLOW_UP_MODEL, systemPrompt, userPrompt, MAX_TOKENS_FOLLOW_UP, TEMPERATURE_FOLLOW_UP);
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

        ClaudeResponse response = executeClaudeRequest(request, "Claude API", maxTokens);
        return response.getContent().get(0).getText();
    }

    private ClaudeResponse executeClaudeRequest(ClaudeRequest request, String apiLabel, int maxTokens) {
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
                            log.warn("[{}] Rate Limited (429)", apiLabel);
                            throw new RetryableApiException(apiLabel + " rate limited (429)");
                        })
                        .onStatus(status -> status.is4xxClientError() && status.value() != 429, (req, res) -> {
                            String body = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "(empty body)";
                            log.error("[{}] 클라이언트 에러: status={}, body={}", apiLabel, res.getStatusCode(), body);
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
                    log.info("[{}] 토큰 사용량 - input: {}, output: {}, cache_write: {}, cache_read: {}",
                            apiLabel, usage.getInputTokens(), usage.getOutputTokens(),
                            usage.getCacheCreationInputTokens(), usage.getCacheReadInputTokens());
                }

                if ("max_tokens".equals(response.getStopReason())) {
                    log.warn("[{}] 응답이 max_tokens({})에 도달하여 잘림. model={}", apiLabel, maxTokens, request.getModel());
                }

                return response;

            } catch (BusinessException e) {
                throw e;
            } catch (RetryableApiException | RestClientException e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.info("[{}] 재시도 {}/{}: {}", apiLabel, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(AiErrorCode.SERVER_ERROR);
                    }
                    delayMs *= 2;
                } else {
                    log.error("[{}] 모든 재시도 실패", apiLabel, e);
                    throw new BusinessException(AiErrorCode.TIMEOUT);
                }
            }
        }
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }
}
