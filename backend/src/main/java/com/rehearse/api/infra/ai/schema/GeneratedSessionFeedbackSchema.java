package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedSessionFeedbackSchema {

    public static final String NAME = "session_feedback";

    public static final List<String> DIMENSION_KEYS = List.of(
            "문제 정의", "기술 깊이", "설명력", "개념 정확도", "실무 응용",
            "경험 구체성", "협업 의식", "답변 회복력", "사실 일관성", "후속 깊이",
            "유창함", "자신감", "시선", "차분함");

    private GeneratedSessionFeedbackSchema() {
    }

    public static Map<String, Object> build() {
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

    public static JsonSchemaSpec spec() {
        return new JsonSchemaSpec(NAME, build());
    }
}
