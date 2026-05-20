package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedQuestionsWrapperSchema {

    public static final String NAME = "generated_questions";

    private GeneratedQuestionsWrapperSchema() {
    }

    public static Map<String, Object> build() {
        Map<String, Object> questionProperties = new LinkedHashMap<>();
        questionProperties.put("content", Map.of("type", "string"));
        questionProperties.put("tts_content", Map.of("type", "string"));
        questionProperties.put("category", Map.of("type", "string"));
        questionProperties.put("order", Map.of("type", "integer"));
        questionProperties.put("evaluation_criteria", Map.of("type", "string"));
        questionProperties.put("question_category", Map.of("type", "string"));
        questionProperties.put("best_answer", Map.of("type", "string"));

        Map<String, Object> questionSchema = new LinkedHashMap<>();
        questionSchema.put("type", "object");
        questionSchema.put("additionalProperties", false);
        questionSchema.put("required", List.of(
                "content", "tts_content", "category", "order",
                "evaluation_criteria", "question_category", "best_answer"));
        questionSchema.put("properties", questionProperties);

        Map<String, Object> questionsArray = new LinkedHashMap<>();
        questionsArray.put("type", "array");
        questionsArray.put("items", questionSchema);

        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("questions", questionsArray);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("questions"));
        schema.put("properties", rootProperties);
        return schema;
    }

    public static JsonSchemaSpec spec() {
        return new JsonSchemaSpec(NAME, build());
    }
}
