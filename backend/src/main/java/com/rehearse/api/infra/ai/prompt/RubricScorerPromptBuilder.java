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
import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScorerPromptBuilder {

    private static final String CALL_TYPE = "rubric_scorer";
    private static final String TEMPLATE_PATH = "classpath:prompts/template/turn-rubric-scorer.txt";
    private static final String SCHEMA_NAME = "rubric_score";

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

        JsonSchemaSpec schemaSpec = new JsonSchemaSpec(SCHEMA_NAME, buildJsonSchema(dimensionsToScore));

        return ChatRequest.builder()
                .messages(messages)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .cachePolicy(CachePolicy.defaults())
                .responseFormat(ResponseFormat.JSON_SCHEMA)
                .jsonSchema(schemaSpec)
                .callType(CALL_TYPE)
                .build();
    }

    Map<String, Object> buildJsonSchema(List<String> dimensionsToScore) {
        Map<String, Object> scoreSchema = new LinkedHashMap<>();
        scoreSchema.put("type", List.of("integer", "null"));
        scoreSchema.put("enum", Arrays.asList(1, 2, 3, null));
        scoreSchema.put("description", "1~3 점 또는 null (차원 무관 시 null)");

        Map<String, Object> observationSchema = new LinkedHashMap<>();
        observationSchema.put("type", "string");
        observationSchema.put("description", "한국어 1~2문장 관찰 서술");

        Map<String, Object> evidenceQuoteSchema = new LinkedHashMap<>();
        evidenceQuoteSchema.put("type", List.of("string", "null"));
        evidenceQuoteSchema.put("description", "사용자 답변에서 추출한 verbatim substring. 차원 무관 시 null");

        Map<String, Object> dimensionProperties = new LinkedHashMap<>();
        dimensionProperties.put("score", scoreSchema);
        dimensionProperties.put("observation", observationSchema);
        dimensionProperties.put("evidence_quote", evidenceQuoteSchema);

        Map<String, Object> dimensionSchema = new LinkedHashMap<>();
        dimensionSchema.put("type", "object");
        dimensionSchema.put("additionalProperties", false);
        dimensionSchema.put("required", List.of("score", "observation", "evidence_quote"));
        dimensionSchema.put("properties", dimensionProperties);

        Map<String, Object> properties = new LinkedHashMap<>();
        for (String dim : dimensionsToScore) {
            properties.put(dim, dimensionSchema);
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.copyOf(dimensionsToScore));
        schema.put("properties", properties);
        return schema;
    }

    private String buildSystemPrompt() {
        return "당신은 한국어로 코칭하는 면접 평가자입니다. observation 은 한국어 1~2문장, evidence_quote 는 사용자 답변 substring 만 인용.\n\n" +
                "## 전체 차원 정의 (참고용 — user 메시지의 DIMENSIONS_TO_SCORE 에 명시된 차원만 채점)\n\n" +
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
