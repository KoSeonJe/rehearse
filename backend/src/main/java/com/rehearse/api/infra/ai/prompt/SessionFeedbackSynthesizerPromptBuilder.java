package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackSynthesizerPromptBuilder {

    private static final String CALL_TYPE = "feedback_synthesizer";
    private static final String TEMPLATE_PATH = "classpath:prompts/template/session-feedback-synthesizer.txt";
    static final String SCHEMA_NAME = "session_feedback";

    static final List<String> DIMENSION_KEYS = List.of(
            "문제 정의", "기술 깊이", "설명력", "개념 정확도", "실무 응용",
            "경험 구체성", "협업 의식", "답변 회복력", "사실 일관성", "후속 깊이",
            "유창함", "자신감", "시선", "차분함");

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${rehearse.feedback-synthesizer.model:gpt-4o-mini}")
    private String model;

    @Value("${rehearse.feedback-synthesizer.temperature:0.4}")
    private double temperature;

    @Value("${rehearse.feedback-synthesizer.max-tokens:2048}")
    private int maxTokens;

    private String template;

    @PostConstruct
    void init() throws IOException {
        var resource = resourceLoader.getResource(TEMPLATE_PATH);
        template = resource.getContentAsString(StandardCharsets.UTF_8);
        log.info("SessionFeedbackSynthesizerPromptBuilder 초기화 완료");
    }

    public ChatRequest build(SessionFeedbackInput input) {
        String userPrompt = buildUserPrompt(input);

        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, userPrompt)))
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .cachePolicy(CachePolicy.defaults())
                .responseFormat(ResponseFormat.JSON_SCHEMA)
                .jsonSchema(new JsonSchemaSpec(SCHEMA_NAME, buildJsonSchema()))
                .callType(CALL_TYPE)
                .build();
    }

    static Map<String, Object> buildJsonSchema() {
        Map<String, Object> dimensionScoresProps = new LinkedHashMap<>();
        for (String key : DIMENSION_KEYS) {
            dimensionScoresProps.put(key, Map.of("type", List.of("number", "null")));
        }
        Map<String, Object> dimensionScoresSchema = new LinkedHashMap<>();
        dimensionScoresSchema.put("type", "object");
        dimensionScoresSchema.put("additionalProperties", false);
        dimensionScoresSchema.put("required", List.copyOf(DIMENSION_KEYS));
        dimensionScoresSchema.put("properties", dimensionScoresProps);

        Map<String, Object> overallProps = new LinkedHashMap<>();
        overallProps.put("dimension_scores", dimensionScoresSchema);
        overallProps.put("level_assessment", Map.of("type", "string"));
        overallProps.put("narrative", Map.of("type", "string"));
        overallProps.put("coverage", Map.of("type", "string"));
        Map<String, Object> overallSchema = new LinkedHashMap<>();
        overallSchema.put("type", "object");
        overallSchema.put("additionalProperties", false);
        overallSchema.put("required",
                List.of("dimension_scores", "level_assessment", "narrative", "coverage"));
        overallSchema.put("properties", overallProps);

        Map<String, Object> strengthProps = new LinkedHashMap<>();
        strengthProps.put("dimension", Map.of("type", "string"));
        strengthProps.put("observation", Map.of("type", "string"));
        strengthProps.put("why_matters", Map.of("type", "string"));
        Map<String, Object> strengthItem = new LinkedHashMap<>();
        strengthItem.put("type", "object");
        strengthItem.put("additionalProperties", false);
        strengthItem.put("required", List.of("dimension", "observation", "why_matters"));
        strengthItem.put("properties", strengthProps);

        Map<String, Object> gapProps = new LinkedHashMap<>();
        gapProps.put("dimension", Map.of("type", "string"));
        gapProps.put("observation", Map.of("type", "string"));
        gapProps.put("level_gap", Map.of("type", "string"));
        gapProps.put("concrete_action", Map.of("type", "string"));
        Map<String, Object> gapItem = new LinkedHashMap<>();
        gapItem.put("type", "object");
        gapItem.put("additionalProperties", false);
        gapItem.put("required", List.of("dimension", "observation", "level_gap", "concrete_action"));
        gapItem.put("properties", gapProps);

        Map<String, Object> deliveryProps = new LinkedHashMap<>();
        deliveryProps.put("filler_words", Map.of("type", "string"));
        deliveryProps.put("tone_pattern", Map.of("type", "string"));
        deliveryProps.put("action", Map.of("type", "string"));
        Map<String, Object> deliveryObject = new LinkedHashMap<>();
        deliveryObject.put("type", "object");
        deliveryObject.put("additionalProperties", false);
        deliveryObject.put("required", List.of("filler_words", "tone_pattern", "action"));
        deliveryObject.put("properties", deliveryProps);
        Map<String, Object> deliverySchema = new LinkedHashMap<>();
        deliverySchema.put("anyOf", List.of(deliveryObject, Map.of("type", "null")));

        Map<String, Object> weekPlanProps = new LinkedHashMap<>();
        weekPlanProps.put("priority", Map.of("type", "integer"));
        weekPlanProps.put("topic", Map.of("type", "string"));
        weekPlanProps.put("resources",
                Map.of("type", "array", "items", Map.of("type", "string")));
        weekPlanProps.put("practice", Map.of("type", "string"));
        Map<String, Object> weekPlanItem = new LinkedHashMap<>();
        weekPlanItem.put("type", "object");
        weekPlanItem.put("additionalProperties", false);
        weekPlanItem.put("required", List.of("priority", "topic", "resources", "practice"));
        weekPlanItem.put("properties", weekPlanProps);

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("overall", overallSchema);
        rootProps.put("strengths", Map.of("type", "array", "items", strengthItem));
        rootProps.put("gaps", Map.of("type", "array", "items", gapItem));
        rootProps.put("delivery", deliverySchema);
        rootProps.put("week_plan", Map.of("type", "array", "items", weekPlanItem));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("overall", "strengths", "gaps", "delivery", "week_plan"));
        schema.put("properties", rootProps);
        return schema;
    }

    private String buildUserPrompt(SessionFeedbackInput input) {
        return template
                .replace("{{SESSION_METADATA}}", serialize(input.sessionMetadata()))
                .replace("{{USER_LEVEL}}", input.userLevel() != null ? input.userLevel().name() : "MID")
                .replace("{{COVERAGE}}", input.coverage() != null ? input.coverage() : "all turns scored")
                .replace("{{TURN_SCORES_JSON}}", serialize(input.turnScores()))
                .replace("{{SCORES_BY_CATEGORY_JSON}}", serialize(input.scoresByCategory()))
                .replace("{{APPLIED_RUBRICS}}", serialize(input.appliedRubrics()))
                .replace("{{DELIVERY_ANALYSIS_JSON}}", nullSafe(input.deliveryAnalysis()))
                .replace("{{VISION_ANALYSIS_JSON}}", nullSafe(input.visionAnalysis()))
                .replace("{{NONVERBAL_AGGREGATE_JSON}}", nonverbalAggregateJson(input));
    }

    private String serialize(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("직렬화 실패: {}", e.getMessage());
            return "null";
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "null";
    }

    private String nonverbalAggregateJson(SessionFeedbackInput input) {
        if (input.nonverbalAggregate() != null) {
            return serialize(input.nonverbalAggregate());
        }
        return nullSafe(input.legacyNonverbalAggregateJson());
    }
}
