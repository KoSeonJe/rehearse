package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedResumeQuestionsSchema {

    public static final String SCHEMA_NAME = "generated_resume_questions";

    private GeneratedResumeQuestionsSchema() {
    }

    private static final List<String> DEPTH_TYPE_ENUM = List.of(
            "TRADEOFF", "LIMITATION", "QUANTITATIVE", "ALTERNATIVE", "PRINCIPLE");

    public static Map<String, Object> build() {
        Map<String, Object> openerProps = new LinkedHashMap<>();
        openerProps.put("question", Map.of("type", "string"));
        openerProps.put("tts_question", Map.of("type", "string"));
        openerProps.put("best_answer", Map.of("type", "string"));

        Map<String, Object> openerItem = new LinkedHashMap<>();
        openerItem.put("type", "object");
        openerItem.put("additionalProperties", false);
        openerItem.put("required", List.of("question", "tts_question", "best_answer"));
        openerItem.put("properties", openerProps);

        Map<String, Object> openerArray = new LinkedHashMap<>();
        openerArray.put("type", "array");
        openerArray.put("items", openerItem);

        Map<String, Object> mainProps = new LinkedHashMap<>();
        mainProps.put("question", Map.of("type", "string"));
        mainProps.put("tts_question", Map.of("type", "string"));
        mainProps.put("best_answer", Map.of("type", "string"));
        mainProps.put("depth_type", Map.of("type", "string", "enum", DEPTH_TYPE_ENUM));

        Map<String, Object> mainItem = new LinkedHashMap<>();
        mainItem.put("type", "object");
        mainItem.put("additionalProperties", false);
        mainItem.put("required", List.of("question", "tts_question", "best_answer", "depth_type"));
        mainItem.put("properties", mainProps);

        Map<String, Object> mainArray = new LinkedHashMap<>();
        mainArray.put("type", "array");
        mainArray.put("items", mainItem);

        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("openers", openerArray);
        rootProps.put("mains", mainArray);

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
