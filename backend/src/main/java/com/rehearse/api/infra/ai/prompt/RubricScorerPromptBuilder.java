package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricDimension;
import com.rehearse.api.domain.feedback.rubric.service.RubricCatalog;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScorerPromptBuilder {

    private static final String CALL_TYPE = "rubric_scorer";
    private static final String TEMPLATE_PATH = "classpath:prompts/template/turn-rubric-scorer.txt";

    private final RubricCatalog rubricLoader;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${rehearse.rubric-scorer.model:gpt-4o-mini}")
    private String model;

    @Value("${rehearse.rubric-scorer.temperature:0.2}")
    private double temperature;

    @Value("${rehearse.rubric-scorer.max-tokens:1536}")
    private int maxTokens;

    private String template;
    private String cachedDimensionDefinitions;

    @PostConstruct
    void init() throws IOException {
        Resource resource = resourceLoader.getResource(TEMPLATE_PATH);
        template = resource.getContentAsString(StandardCharsets.UTF_8);
        cachedDimensionDefinitions = buildDimensionDefinitions();
        log.info("RubricScorerPromptBuilder 초기화 완료");
    }

    public ChatRequest build(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel userLevel
    ) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(question, userAnswer, analysis, rubric,
                dimensionsToScore, userLevel);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, systemPrompt));
        messages.add(ChatMessage.of(ChatMessage.Role.USER, userPrompt));

        return ChatRequest.builder()
                .messages(messages)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .cachePolicy(CachePolicy.defaults())
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType(CALL_TYPE)
                .build();
    }

    private String buildSystemPrompt() {
        return "You are an expert technical interview evaluator.\n\n" +
                "## All Dimension Definitions (reference only — score ONLY the dimensions listed in the user message)\n\n" +
                cachedDimensionDefinitions;
    }

    private String buildUserPrompt(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel userLevel
    ) {
        return template
                .replace("{{USER_LEVEL}}", formatLevel(userLevel))
                .replace("{{QUESTION_TEXT}}", question.getQuestionText())
                .replace("{{USER_ANSWER}}", userAnswer != null ? userAnswer : "")
                .replace("{{ANSWER_ANALYSIS_JSON}}", serializeAnalysis(analysis))
                .replace("{{DIMENSIONS_TO_SCORE}}", String.join(", ", dimensionsToScore))
                .replace("{{DIMENSION_DEFINITIONS}}", buildSelectedDefinitions(dimensionsToScore))
                .replace("{{RESUME_CONTEXT}}", "")
                .replace("{{CHAIN_DEPTH_OVERRIDE}}", "");
    }

    private String buildDimensionDefinitions() {
        Map<String, RubricDimension> dimensions = rubricLoader.getAllDimensions();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, RubricDimension> entry : dimensions.entrySet()) {
            RubricDimension dim = entry.getValue();
            sb.append("### ").append(entry.getKey()).append(" — ").append(dim.name()).append("\n");
            sb.append(dim.description()).append("\n");
            if (dim.scoring() != null) {
                for (Map.Entry<Integer, RubricDimension.ScoringLevel> se : dim.scoring().entrySet()) {
                    sb.append("- Score ").append(se.getKey()).append(" (").append(se.getValue().label()).append(")");
                    if (se.getValue().observable() != null && !se.getValue().observable().isEmpty()) {
                        sb.append(": ").append(String.join("; ", se.getValue().observable()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildSelectedDefinitions(List<String> dimensionsToScore) {
        Map<String, RubricDimension> dimensions = rubricLoader.getAllDimensions();
        StringBuilder sb = new StringBuilder();
        for (String ref : dimensionsToScore) {
            RubricDimension dim = dimensions.get(ref);
            if (dim == null) {
                continue;
            }
            sb.append("### ").append(ref).append(" — ").append(dim.name()).append("\n");
            sb.append(dim.description()).append("\n");
            if (dim.scoring() != null) {
                for (Map.Entry<Integer, RubricDimension.ScoringLevel> se : dim.scoring().entrySet()) {
                    sb.append("- Score ").append(se.getKey()).append(" (").append(se.getValue().label()).append(")");
                    if (se.getValue().observable() != null && !se.getValue().observable().isEmpty()) {
                        sb.append(": ").append(String.join("; ", se.getValue().observable()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatLevel(InterviewLevel level) {
        if (level == null) return "MID";
        return switch (level) {
            case JUNIOR -> "JUNIOR";
            case MID -> "MID";
            case SENIOR -> "SENIOR";
        };
    }

    private String serializeAnalysis(AnswerAnalysis analysis) {
        if (analysis == null) return "{}";
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            log.warn("AnswerAnalysis 직렬화 실패", e);
            return "{}";
        }
    }
}
