package com.devlens.api.infra.ai;

import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.domain.interview.entity.Position;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
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

        String text = callClaudeApi(systemPrompt, userPrompt, 4096);
        GeneratedQuestionsWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedQuestionsWrapper.class);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI가 생성한 질문을 파싱할 수 없습니다.");
        }

        return wrapper.getQuestions();
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(String questionContent, String answerText, String nonVerbalSummary) {
        String systemPrompt = promptBuilder.buildFollowUpSystemPrompt();
        String userPrompt = promptBuilder.buildFollowUpUserPrompt(questionContent, answerText, nonVerbalSummary);

        String text = callClaudeApi(systemPrompt, userPrompt, 1024);
        return responseParser.parseJsonResponse(text, GeneratedFollowUp.class);
    }

    @Override
    public GeneratedReport generateReport(String feedbackSummary) {
        String systemPrompt = promptBuilder.buildReportSystemPrompt();
        String userPrompt = promptBuilder.buildReportUserPrompt(feedbackSummary);

        String text = callClaudeApi(systemPrompt, userPrompt, 2048);
        return responseParser.parseJsonResponse(text, GeneratedReport.class);
    }

    @Override
    public List<GeneratedFeedback> generateFeedback(String answersJson) {
        String systemPrompt = promptBuilder.buildFeedbackSystemPrompt();
        String userPrompt = promptBuilder.buildFeedbackUserPrompt(answersJson);

        String text = callClaudeApi(systemPrompt, userPrompt, 4096);
        GeneratedFeedbackWrapper wrapper = responseParser.parseJsonResponse(text, GeneratedFeedbackWrapper.class);

        if (wrapper.getFeedbacks() == null || wrapper.getFeedbacks().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_006", "AI 피드백을 생성할 수 없습니다.");
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
                        throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_001", "AI 요청에 실패했습니다: " + body);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Claude API 서버 에러: status={}", res.getStatusCode());
                        throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_002", "AI 서비스가 일시적으로 불안정합니다.");
                    })
                    .body(ClaudeResponse.class);

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_003", "AI 응답이 비어있습니다.");
            }

            return response.getContent().get(0).getText();

        } catch (RestClientException e) {
            log.error("Claude API 호출 실패", e);
            throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, "AI_004", "AI 서비스 호출 시간이 초과되었습니다.");
        }
    }
}
