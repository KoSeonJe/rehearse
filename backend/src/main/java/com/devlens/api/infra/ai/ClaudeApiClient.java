package com.devlens.api.infra.ai;

import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class ClaudeApiClient {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public ClaudeApiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.model:claude-sonnet-4-20250514}") String model) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl(ANTHROPIC_API_URL)
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<GeneratedQuestion> generateQuestions(String position, InterviewLevel level, InterviewType interviewType) {
        String systemPrompt = buildQuestionSystemPrompt();
        String userPrompt = buildQuestionUserPrompt(position, level, interviewType);

        String text = callClaudeApi(systemPrompt, userPrompt, 2048);
        GeneratedQuestionsWrapper wrapper = parseJsonResponse(text, GeneratedQuestionsWrapper.class);

        if (wrapper.getQuestions() == null || wrapper.getQuestions().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI가 생성한 질문을 파싱할 수 없습니다.");
        }

        return wrapper.getQuestions();
    }

    public GeneratedFollowUp generateFollowUpQuestion(String questionContent, String answerText, String nonVerbalSummary) {
        String systemPrompt = """
                당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
                면접자의 답변을 바탕으로 더 깊이 있는 후속 질문을 생성합니다.

                후속 질문 유형:
                - DEEP_DIVE: 답변의 특정 부분을 더 깊이 파고드는 질문
                - CLARIFICATION: 모호한 답변을 명확히 하기 위한 질문
                - CHALLENGE: 답변의 논리적 허점이나 대안을 탐색하는 질문
                - APPLICATION: 답변 내용을 다른 상황에 적용해보는 질문

                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "question": "후속 질문 내용",
                  "reason": "이 질문을 하는 이유",
                  "type": "DEEP_DIVE|CLARIFICATION|CHALLENGE|APPLICATION"
                }
                """;

        String userPrompt = String.format("""
                원래 질문: %s
                면접자 답변: %s
                비언어적 관찰: %s

                위 답변에 대한 후속 질문을 생성해주세요.
                """, questionContent, answerText,
                nonVerbalSummary != null ? nonVerbalSummary : "관찰 데이터 없음");

        String text = callClaudeApi(systemPrompt, userPrompt, 1024);
        return parseJsonResponse(text, GeneratedFollowUp.class);
    }

    public List<GeneratedFeedback> generateFeedback(String answersJson) {
        String systemPrompt = """
                당신은 면접 코치입니다. 면접자의 답변과 비언어적 데이터를 분석하여 타임스탬프 기반 피드백을 생성합니다.

                피드백 카테고리:
                - VERBAL: 언어적 피드백 (답변 내용, 논리성, 구체성)
                - NON_VERBAL: 비언어적 피드백 (시선, 자세, 표정)
                - CONTENT: 내용 피드백 (기술적 정확성, 깊이)

                심각도:
                - INFO: 긍정적 관찰 또는 중립 정보
                - WARNING: 개선이 필요한 주의 사항
                - SUGGESTION: 구체적 개선 제안

                반드시 아래 JSON 형식으로만 응답하세요:
                {
                  "feedbacks": [
                    {
                      "timestampSeconds": 0.0,
                      "category": "VERBAL|NON_VERBAL|CONTENT",
                      "severity": "INFO|WARNING|SUGGESTION",
                      "content": "피드백 내용",
                      "suggestion": "개선 제안 (선택)"
                    }
                  ]
                }
                """;

        String userPrompt = String.format("""
                아래는 면접 답변 데이터입니다. 각 답변에 대해 타임스탬프 기반 피드백을 생성해주세요.

                %s
                """, answersJson);

        String text = callClaudeApi(systemPrompt, userPrompt, 4096);
        GeneratedFeedbackWrapper wrapper = parseJsonResponse(text, GeneratedFeedbackWrapper.class);

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

    private <T> T parseJsonResponse(String text, Class<T> clazz) {
        try {
            String json = extractJson(text);
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Claude 응답 JSON 파싱 실패: {}", text, e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 응답을 파싱할 수 없습니다.");
        }
    }

    private String extractJson(String text) {
        String json = text;
        if (json.contains("```json")) {
            json = json.substring(json.indexOf("```json") + 7);
            json = json.substring(0, json.indexOf("```"));
        } else if (json.contains("```")) {
            json = json.substring(json.indexOf("```") + 3);
            json = json.substring(0, json.indexOf("```"));
        }
        return json.trim();
    }

    private String buildQuestionSystemPrompt() {
        return """
                당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
                주어진 직무, 레벨, 면접 유형에 맞는 면접 질문을 생성해야 합니다.

                면접 유형별 가이드:
                - CS: 자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스 관련 기초 질문
                - SYSTEM_DESIGN: 시스템 스케일링, 아키텍처 설계, 트레이드오프 분석 질문
                - BEHAVIORAL: STAR 기법 기반 경험 질문 (상황, 과제, 행동, 결과)

                레벨별 난이도:
                - JUNIOR: 기본 개념 이해도 확인, 실무 경험보다 학습 의지
                - MID: 실무 적용 능력, 문제 해결 경험, 기술적 깊이
                - SENIOR: 아키텍처 판단력, 리더십, 기술 의사결정 능력

                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
                {
                  "questions": [
                    {
                      "content": "질문 내용",
                      "category": "세부 카테고리명",
                      "order": 1,
                      "evaluationCriteria": "이 질문에서 평가할 핵심 포인트"
                    }
                  ]
                }
                """;
    }

    private String buildQuestionUserPrompt(String position, InterviewLevel level, InterviewType interviewType) {
        String levelKorean = switch (level) {
            case JUNIOR -> "주니어";
            case MID -> "미드레벨";
            case SENIOR -> "시니어";
        };

        String typeKorean = switch (interviewType) {
            case CS -> "CS 기초";
            case SYSTEM_DESIGN -> "시스템 설계";
            case BEHAVIORAL -> "Behavioral (인성/경험)";
        };

        return String.format("""
                직무: %s
                레벨: %s
                면접 유형: %s

                위 조건에 맞는 면접 질문 5개와 각 질문별 평가 기준을 생성해주세요.
                각 질문의 카테고리는 면접 유형의 세부 분야로 지정해주세요.
                """, position, levelKorean, typeKorean);
    }
}
