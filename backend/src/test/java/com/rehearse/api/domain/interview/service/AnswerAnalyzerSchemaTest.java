package com.rehearse.api.domain.interview.service;

import com.rehearse.api.infra.ai.schema.GeneratedAnswerAnalysisSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedAnswerAnalysisSchema — 트랙별 strict JSON Schema 정의 검증")
class AnswerAnalyzerSchemaTest {

    @Test
    @DisplayName("CS 트랙 spec name 은 answer_analysis_cs, Resume 트랙은 answer_analysis_resume")
    void spec_name_differs_per_track() {
        assertThat(GeneratedAnswerAnalysisSchema.spec(false).name()).isEqualTo("answer_analysis_cs");
        assertThat(GeneratedAnswerAnalysisSchema.spec(true).name()).isEqualTo("answer_analysis_resume");
    }

    @Nested
    @DisplayName("공통 root 구조")
    class Root {

        @Test
        @DisplayName("CS / Resume 모두 root 는 6개 필드 required + additionalProperties=false")
        void root_requiresAllTopLevelFields() {
            for (boolean resumeTrack : List.of(false, true)) {
                Map<String, Object> schema = GeneratedAnswerAnalysisSchema.build(resumeTrack);

                assertThat(schema).containsEntry("type", "object");
                assertThat(schema).containsEntry("additionalProperties", false);
                assertThat(schema.get("required")).isEqualTo(List.of(
                        "transcript", "claims", "dimension_gaps", "weakest_dimension",
                        "unstated_assumptions", "recommended_next_action"));
            }
        }
    }

    @Nested
    @DisplayName("dimension_gaps — 트랙별 키 집합 다름, 모두 0~3 enum 정수")
    class DimensionGaps {

        @Test
        @DisplayName("CS 트랙은 8개 키만 required (factual_consistency / chain_depth 제외)")
        void csTrack_has8Keys() {
            Map<String, Object> dimensionGaps = dimensionGaps(false);

            assertThat(dimensionGaps.get("required"))
                    .isEqualTo(GeneratedAnswerAnalysisSchema.CS_DIMENSION_KEYS);
            assertThat(GeneratedAnswerAnalysisSchema.CS_DIMENSION_KEYS).hasSize(8)
                    .doesNotContain("factual_consistency", "chain_depth");
        }

        @Test
        @DisplayName("Resume 트랙은 10개 키 required (CS 8개 + factual_consistency + chain_depth)")
        void resumeTrack_has10Keys() {
            Map<String, Object> dimensionGaps = dimensionGaps(true);

            assertThat(dimensionGaps.get("required"))
                    .isEqualTo(GeneratedAnswerAnalysisSchema.RESUME_DIMENSION_KEYS);
            assertThat(GeneratedAnswerAnalysisSchema.RESUME_DIMENSION_KEYS).hasSize(10)
                    .contains("factual_consistency", "chain_depth");
        }

        @Test
        @DisplayName("각 dimension 값은 0~3 정수 enum, null 불허")
        void dimensionValue_isIntegerEnum0to3_nullForbidden() {
            for (boolean resumeTrack : List.of(false, true)) {
                Map<String, Object> dimensionGaps = dimensionGaps(resumeTrack);

                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) dimensionGaps.get("properties");
                for (var entry : props.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> field = (Map<String, Object>) entry.getValue();
                    assertThat(field.get("type")).isEqualTo("integer");
                    assertThat(field.get("enum")).isEqualTo(List.of(0, 1, 2, 3));
                }
            }
        }
    }

    @Test
    @DisplayName("claims item 은 4개 필드 required + evidence_strength enum 제한")
    void buildJsonSchema_claimItem_strict() {
        Map<String, Object> schema = GeneratedAnswerAnalysisSchema.build(false);

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
        Map<String, Object> schema = GeneratedAnswerAnalysisSchema.build(false);

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) properties.get("recommended_next_action");

        assertThat(action.get("enum")).isEqualTo(List.of(
                "DEEP_DIVE", "CLARIFICATION", "CHALLENGE", "APPLICATION", "SKIP"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dimensionGaps(boolean resumeTrack) {
        Map<String, Object> schema = GeneratedAnswerAnalysisSchema.build(resumeTrack);
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        return (Map<String, Object>) properties.get("dimension_gaps");
    }
}
