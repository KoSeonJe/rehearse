package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedResumeQuestionsSchema {

    public static final String SCHEMA_NAME = "generated_resume_questions";

    private GeneratedResumeQuestionsSchema() {
    }

    public static Map<String, Object> build() {
        Map<String, Object> questionProps = new LinkedHashMap<>();
        questionProps.put("question", Map.of("type", "string"));
        questionProps.put("tts_question", Map.of("type", "string"));
        questionProps.put("best_answer", Map.of("type", "string"));

        Map<String, Object> questionItem = new LinkedHashMap<>();
        questionItem.put("type", "object");
        questionItem.put("additionalProperties", false);
        questionItem.put("required", List.of("question", "tts_question", "best_answer"));
        questionItem.put("properties", questionProps);

        Map<String, Object> questionArray = new LinkedHashMap<>();
        questionArray.put("type", "array");
        questionArray.put("items", questionItem);

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("openers", questionArray);
        rootProps.put("mains", questionArray);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("openers", "mains"));
        schema.put("properties", rootProps);
        return schema;
    }

    public static JsonSchemaSpec spec() {
        return new JsonSchemaSpec(SCHEMA_NAME, build());
    }
}
