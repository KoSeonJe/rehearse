package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedAnswerAnalysisSchema {

    public static final String NAME = "answer_analysis";

    public static final List<String> DIMENSION_KEYS = List.of(
            "problem_framing", "technical_depth", "reasoning_communication",
            "conceptual_accuracy", "practical_application", "experience_concreteness",
            "collaboration_awareness", "recovery_from_gaps",
            "factual_consistency", "chain_depth");

    private static final List<String> EVIDENCE_STRENGTHS = List.of("STRONG", "WEAK", "ASSUMED");
    private static final List<String> RECOMMENDED_ACTIONS = List.of(
            "DEEP_DIVE", "CLARIFICATION", "CHALLENGE", "APPLICATION", "SKIP");

    private GeneratedAnswerAnalysisSchema() {
    }

    public static Map<String, Object> build() {
        Map<String, Object> claimProps = new LinkedHashMap<>();
        claimProps.put("text", Map.of("type", "string"));
        claimProps.put("depth_score", Map.of("type", "integer"));
        claimProps.put("evidence_strength", Map.of("type", "string", "enum", EVIDENCE_STRENGTHS));
        claimProps.put("topic_tag", Map.of("type", "string"));

        Map<String, Object> claimItem = new LinkedHashMap<>();
        claimItem.put("type", "object");
        claimItem.put("additionalProperties", false);
        claimItem.put("required", List.of("text", "depth_score", "evidence_strength", "topic_tag"));
        claimItem.put("properties", claimProps);

        Map<String, Object> dimensionGapsProps = new LinkedHashMap<>();
        for (String key : DIMENSION_KEYS) {
            dimensionGapsProps.put(key, Map.of("type", List.of("integer", "null")));
        }
        Map<String, Object> dimensionGapsSchema = new LinkedHashMap<>();
        dimensionGapsSchema.put("type", "object");
        dimensionGapsSchema.put("additionalProperties", false);
        dimensionGapsSchema.put("required", List.copyOf(DIMENSION_KEYS));
        dimensionGapsSchema.put("properties", dimensionGapsProps);

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("claims", Map.of("type", "array", "items", claimItem));
        rootProps.put("dimension_gaps", dimensionGapsSchema);
        rootProps.put("weakest_dimension", Map.of("type", List.of("string", "null")));
        rootProps.put("unstated_assumptions",
                Map.of("type", "array", "items", Map.of("type", "string")));
        rootProps.put("recommended_next_action",
                Map.of("type", "string", "enum", RECOMMENDED_ACTIONS));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "claims", "dimension_gaps", "weakest_dimension",
                "unstated_assumptions", "recommended_next_action"));
        schema.put("properties", rootProps);
        return schema;
    }

    public static JsonSchemaSpec spec() {
        return new JsonSchemaSpec(NAME, build());
    }
}
