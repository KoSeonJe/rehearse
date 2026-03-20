package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
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

    private static final int MAX_TOKENS_QUESTION = 4096;
    private static final int MAX_TOKENS_FOLLOW_UP = 1024;
    private static final int MAX_TOKENS_REPORT = 2048;

    private final RestClient restClient;
    private final ClaudePromptBuilder promptBuilder;
    private final ClaudeResponseParser responseParser;
    private final String apiKey;
    private final String model;

    public ClaudeApiClient(
            RestClient.Builder restClientBuilder,
            ClaudePromptBuilder promptBuilder,
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
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(Position position, String positionDetail,
                                                      InterviewLevel level, List<InterviewType> interviewTypes,
                                                      List<String> csSubTopics, String resumeText,
                                                      Integer durationMinutes) {
        String systemPrompt = promptBuilder.buildQuestionSystemPrompt();
        String userPrompt = promptBuilder.buildQuestionUserPrompt(position, positionDetail, level, interviewTypes, csSubTopics, resumeText, durationMinutes);

        String text = callClaudeApi(systemPrompt, userPrompt, MAX_TOKENS_QUESTION, 0.9);
        GeneratedQuestionsWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedQuestionsWrapper.class);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        return wrapper.getQuestions();
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(String questionContent, String answerText,
                                                       String nonVerbalSummary,
                                                       List<FollowUpRequest.FollowUpExchange> previousExchanges) {
        String systemPrompt = promptBuilder.buildFollowUpSystemPrompt();
        String userPrompt = promptBuilder.buildFollowUpUserPrompt(questionContent, answerText, nonVerbalSummary, previousExchanges);

        String text = callClaudeApi(systemPrompt, userPrompt, MAX_TOKENS_FOLLOW_UP);
        return responseParser.parseJsonResponse(text, GeneratedFollowUp.class);
    }

    @Override
    public GeneratedReport generateReport(String feedbackSummary) {
        String systemPrompt = promptBuilder.buildReportSystemPrompt();
        String userPrompt = promptBuilder.buildReportUserPrompt(feedbackSummary);

        String text = callClaudeApi(systemPrompt, userPrompt, MAX_TOKENS_REPORT);
        return responseParser.parseJsonResponse(text, GeneratedReport.class);
    }

    private String callClaudeApi(String systemPrompt, String userPrompt, int maxTokens) {
        return callClaudeApi(systemPrompt, userPrompt, maxTokens, null);
    }

    private String callClaudeApi(String systemPrompt, String userPrompt, int maxTokens, Double temperature) {
        ClaudeRequest request = ClaudeRequest.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(systemPrompt)
                .messages(List.of(
                        ClaudeRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .temperature(temperature)
                .build();

        int maxAttempts = 3;
        long delayMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
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
                    log.info("[Claude API] 토큰 사용량 - input: {}, output: {}, total: {}",
                            response.getUsage().getInputTokens(),
                            response.getUsage().getOutputTokens(),
                            response.getUsage().getInputTokens() + response.getUsage().getOutputTokens());
                }

                return response.getContent().get(0).getText();

            } catch (BusinessException e) {
                throw e;
            } catch (RetryableApiException | RestClientException e) {
                if (attempt < maxAttempts) {
                    log.info("[Claude API] 재시도 {}/{}: {}", attempt, maxAttempts, e.getMessage());
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
        // 컴파일러 요구사항 — 실제로 도달하지 않음
        throw new BusinessException(AiErrorCode.TIMEOUT);
    }
}
