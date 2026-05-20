package com.rehearse.api.domain.interview.service;

import com.rehearse.api.infra.ai.schema.GeneratedAnswerAnalysisSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedAnswerAnalysisSchema — strict JSON Schema 정의 검증")
class AnswerAnalyzerSchemaTest {

    @Test
    @DisplayName("build() root 는 5개 필드 모두 required + additionalProperties=false")
    void buildJsonSchema_root_requiresAllTopLevelFields() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) GeneratedAnswerAnalysisSchema.build();

        assertThat(schema).containsEntry("type", "object");
        assertThat(schema).containsEntry("additionalProperties", false);
        assertThat(schema.get("required")).isEqualTo(List.of(
                "claims", "dimension_gaps", "weakest_dimension",
                "unstated_assumptions", "recommended_next_action"));
    }

    @Test
    @DisplayName("dimension_gaps 는 10개 dimension id 모두를 required 로 포함하고 각 값은 nullable integer")
    void buildJsonSchema_dimensionGaps_requiresAll10Keys_asNullableInteger() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) GeneratedAnswerAnalysisSchema.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> dimensionGaps = (Map<String, Object>) properties.get("dimension_gaps");

        assertThat(dimensionGaps).containsEntry("type", "object");
        assertThat(dimensionGaps).containsEntry("additionalProperties", false);
        assertThat(dimensionGaps.get("required"))
                .isEqualTo(List.copyOf(GeneratedAnswerAnalysisSchema.DIMENSION_KEYS));

        @SuppressWarnings("unchecked")
        Map<String, Object> dimensionGapsProps = (Map<String, Object>) dimensionGaps.get("properties");
        for (String key : GeneratedAnswerAnalysisSchema.DIMENSION_KEYS) {
            @SuppressWarnings("unchecked")
            Map<String, Object> field = (Map<String, Object>) dimensionGapsProps.get(key);
            assertThat(field.get("type")).isEqualTo(List.of("integer", "null"));
        }
    }

    @Test
    @DisplayName("claims item 은 4개 필드 required + evidence_strength enum 제한")
    void buildJsonSchema_claimItem_strict() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) GeneratedAnswerAnalysisSchema.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) properties.get("claims");
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) claims.get("items");

        assertThat(item).containsEntry("type", "object");
        assertThat(item).containsEntry("additionalProperties", false);
        assertThat(item.get("required")).isEqualTo(List.of(
                "text", "depth_score", "evidence_strength", "topic_tag"));

        @SuppressWarnings("unchecked")
        Map<String, Object> itemProps = (Map<String, Object>) item.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> evidence = (Map<String, Object>) itemProps.get("evidence_strength");
        assertThat(evidence.get("enum")).isEqualTo(List.of("STRONG", "WEAK", "ASSUMED"));
    }

    @Test
    @DisplayName("recommended_next_action 은 5개 enum 으로 제한된다")
    void buildJsonSchema_recommendedNextAction_enum() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) GeneratedAnswerAnalysisSchema.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) properties.get("recommended_next_action");

        assertThat(action.get("enum")).isEqualTo(List.of(
                "DEEP_DIVE", "CLARIFICATION", "CHALLENGE", "APPLICATION", "SKIP"));
    }
}
