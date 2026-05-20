package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedFollowUpSchema {

    public static final String SCHEMA_NAME = "generated_follow_up";

    private GeneratedFollowUpSchema() {
    }

    public static Map<String, Object> build() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("skip", Map.of("type", "boolean"));
        properties.put("skip_reason", nullableString());
        properties.put("question", nullableString());
        properties.put("tts_question", nullableString());
        properties.put("reason", nullableString());
        properties.put("type", nullableString());
        properties.put("best_answer", nullableString());
        properties.put("answer_text", Map.of("type", "string"));
        properties.put("target_claim_idx", Map.of("type", List.of("integer", "null")));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of(
                "skip", "skip_reason", "question", "tts_question",
                "reason", "type", "best_answer", "answer_text", "target_claim_idx"));
        schema.put("properties", properties);
        return schema;
    }

    public static JsonSchemaSpec spec() {
        return new JsonSchemaSpec(SCHEMA_NAME, build());
    }

    private static Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }
}
