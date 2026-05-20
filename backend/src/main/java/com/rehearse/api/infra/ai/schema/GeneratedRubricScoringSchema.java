package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedRubricScoringSchema {

    public static final String SCHEMA_NAME = "rubric_score";

    private GeneratedRubricScoringSchema() {
    }

    public static Map<String, Object> build(List<String> dimensionsToScore) {
        Map<String, Object> scoreSchema = new LinkedHashMap<>();
        scoreSchema.put("type", "integer");
        scoreSchema.put("enum", List.of(1, 2, 3));
        scoreSchema.put("description", "1~3 점 (모든 차원 강제 채점)");

        Map<String, Object> observationSchema = new LinkedHashMap<>();
        observationSchema.put("type", "string");
        observationSchema.put("description", "한국어 1~2문장 관찰 서술");

        Map<String, Object> evidenceQuoteSchema = new LinkedHashMap<>();
        evidenceQuoteSchema.put("type", "string");
        evidenceQuoteSchema.put("description", "사용자 답변에서 추출한 verbatim substring. 답변에 관련 발언이 전혀 없으면 정확히 \"관련 발언 없음\" 고정 문구 사용");

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

    public static JsonSchemaSpec spec(List<String> dimensionsToScore) {
        return new JsonSchemaSpec(SCHEMA_NAME, build(dimensionsToScore));
    }
}
