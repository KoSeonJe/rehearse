package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
    private static final int MAX_TOKENS_FEEDBACK = 4096;

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

        String text = callClaudeApi(systemPrompt, userPrompt, MAX_TOKENS_QUESTION);
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

    @Override
    public List<GeneratedFeedback> generateFeedback(String answersJson) {
        String systemPrompt = promptBuilder.buildFeedbackSystemPrompt();
        String userPrompt = promptBuilder.buildFeedbackUserPrompt(answersJson);

        String text = callClaudeApi(systemPrompt, userPrompt, MAX_TOKENS_FEEDBACK);
        GeneratedFeedbackWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedFeedbackWrapper.class);

        if (wrapper.getFeedbacks() == null || wrapper.getFeedbacks().isEmpty()) {
            throw new BusinessException(AiErrorCode.FEEDBACK_PARSE_FAILED);
        }

        return wrapper.getFeedbacks();
    }

    private String callClaudeApi(String systemPrompt, String userPrompt, int maxTokens) {
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
                .build();

        try {
            ClaudeResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        log.error("Claude API 클라이언트 에러: status={}, body={}", res.getStatusCode(), body);
                        throw new BusinessException(AiErrorCode.CLIENT_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Claude API 서버 에러: status={}", res.getStatusCode());
                        throw new BusinessException(AiErrorCode.SERVER_ERROR);
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

        } catch (RestClientException e) {
            log.error("Claude API 호출 실패", e);
            throw new BusinessException(AiErrorCode.TIMEOUT);
        }
    }
}
