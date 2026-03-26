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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiClient {

    private static final int MAX_TOKENS_QUESTION = 8192;
    private static final int MAX_TOKENS_FOLLOW_UP = 1024;

    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final RestClient restClient;
    private final QuestionGenerationPromptBuilder questionPromptBuilder;
    private final FollowUpPromptBuilder followUpPromptBuilder;
    private final AiResponseParser responseParser;
    private final String apiKey;
    private final String model;

    public OpenAiClient(
            RestClient.Builder restClientBuilder,
            QuestionGenerationPromptBuilder questionPromptBuilder,
            FollowUpPromptBuilder followUpPromptBuilder,
            AiResponseParser responseParser,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        this.questionPromptBuilder = questionPromptBuilder;
        this.followUpPromptBuilder = followUpPromptBuilder;
        this.responseParser = responseParser;
        this.apiKey = apiKey;
        this.model = model;
    }

    @RateLimiter(name = "openai-api")
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        String systemPrompt = questionPromptBuilder.buildSystemPrompt(request);
        String userPrompt = questionPromptBuilder.buildUserPrompt(request);

        String text = callOpenAiApi(systemPrompt, userPrompt, MAX_TOKENS_QUESTION, 0.9);
        GeneratedQuestionsWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedQuestionsWrapper.class);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        return wrapper.getQuestions();
    }

    @RateLimiter(name = "openai-api")
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        String systemPrompt = followUpPromptBuilder.buildSystemPrompt(request);
        String userPrompt = followUpPromptBuilder.buildUserPrompt(request);

        String text = callOpenAiApi(systemPrompt, userPrompt, MAX_TOKENS_FOLLOW_UP, 1.0);
        return responseParser.parseJsonResponse(text, GeneratedFollowUp.class);
    }

    private String callOpenAiApi(String systemPrompt, String userPrompt, int maxTokens, Double temperature) {
        OpenAiRequest request = OpenAiRequest.builder()
                .model(model)
                .messages(List.of(
                        OpenAiRequest.Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        OpenAiRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        long delayMs = INITIAL_RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                OpenAiResponse response = restClient.post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(request)
                        .retrieve()
                        .onStatus(status -> status.value() == 429, (req, res) -> {
                            log.warn("[OpenAI API] Rate Limited (429)");
                            throw new RetryableApiException("OpenAI API rate limited (429)");
                        })
                        .onStatus(status -> status.is4xxClientError() && status.value() != 429, (req, res) -> {
                            String body = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "(empty body)";
                            log.error("[OpenAI API] 클라이언트 에러: status={}, body={}", res.getStatusCode(), body);
                            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                            log.warn("[OpenAI API] 서버 에러: status={}", res.getStatusCode());
                            throw new RetryableApiException("OpenAI API 서버 에러: " + res.getStatusCode());
                        })
                        .body(OpenAiResponse.class);

                if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                    throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
                }

                if (response.getUsage() != null) {
                    var usage = response.getUsage();
                    log.info("[OpenAI API] 토큰 사용량 - prompt: {}, completion: {}, total: {}",
                            usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
                }

                OpenAiResponse.Choice choice = response.getChoices().get(0);
                if ("length".equals(choice.getFinishReason())) {
                    log.warn("[OpenAI API] 응답이 max_tokens({})에 도달하여 잘림. model={}", maxTokens, model);
                }

                String content = choice.getMessage() != null ? choice.getMessage().getContent() : null;
                if (content == null || content.isBlank()) {
                    throw new BusinessException(AiErrorCode.EMPTY_RESPONSE);
                }
                return content;

            } catch (BusinessException e) {
                throw e;
            } catch (RetryableApiException | RestClientException e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.info("[OpenAI API] 재시도 {}/{}: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(AiErrorCode.SERVER_ERROR);
                    }
                    delayMs *= 2;
                } else {
                    log.error("[OpenAI API] 모든 재시도 실패", e);
                    throw new BusinessException(AiErrorCode.TIMEOUT);
                }
            }
        }
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }
}
