package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SchemaExampleRegistry 예시는 strict schema 의 required 키 집합과 정확히 일치한다")
class SchemaExampleRegistryTest {

    private static final Set<String> ANSWER_ANALYSIS_ROOT_KEYS = Set.of(
            "claims", "dimension_gaps", "weakest_dimension",
            "unstated_assumptions", "recommended_next_action");

    private static final Set<String> ANSWER_ANALYSIS_DIMENSION_KEYS = new LinkedHashSet<>(List.of(
            "problem_framing", "technical_depth", "reasoning_communication",
            "conceptual_accuracy", "practical_application", "experience_concreteness",
            "collaboration_awareness", "recovery_from_gaps",
            "factual_consistency", "chain_depth"));

    private static final Set<String> FOLLOW_UP_REQUIRED_KEYS = Set.of(
            "skip", "skip_reason", "question", "tts_question",
            "reason", "type", "best_answer", "answer_text", "target_claim_idx");

    private static final Set<String> SESSION_FEEDBACK_ROOT_KEYS = Set.of(
            "overall", "strengths", "gaps", "delivery", "week_plan");

    private static final Set<String> SESSION_FEEDBACK_OVERALL_KEYS = Set.of(
            "dimension_scores", "level_assessment", "narrative", "coverage");

    private static final List<String> SESSION_FEEDBACK_DIMENSION_KEYS = List.of(
            "문제 정의", "기술 깊이", "설명력", "개념 정확도", "실무 응용",
            "경험 구체성", "협업 의식", "답변 회복력", "사실 일관성", "후속 깊이",
            "유창함", "자신감", "시선", "차분함");

    private final SchemaExampleRegistry registry = new SchemaExampleRegistry();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("AnswerAnalysis 예시")
    class AnswerAnalysisExample {

        @Test
        @DisplayName("루트 키 집합이 schema required 와 일치한다")
        void rootKeys_match_schema_required() throws Exception {
            JsonNode root = parse(GeneratedAnswerAnalysis.class);

            assertThat(fieldNames(root)).isEqualTo(ANSWER_ANALYSIS_ROOT_KEYS);
        }

        @Test
        @DisplayName("dimension_gaps 키가 AnswerAnalyzer.DIMENSION_KEYS 10개와 정확히 일치한다")
        void dimensionGaps_keys_match_dimension_keys() throws Exception {
            JsonNode root = parse(GeneratedAnswerAnalysis.class);

            assertThat(fieldNames(root.get("dimension_gaps")))
                    .isEqualTo(ANSWER_ANALYSIS_DIMENSION_KEYS);
        }
    }

    @Nested
    @DisplayName("GeneratedFollowUp 예시")
    class FollowUpExample {

        @Test
        @DisplayName("루트 키 집합이 schema required 9개 (snake_case) 와 일치한다")
        void rootKeys_match_schema_required() throws Exception {
            JsonNode root = parse(GeneratedFollowUp.class);

            assertThat(fieldNames(root)).isEqualTo(FOLLOW_UP_REQUIRED_KEYS);
        }
    }

    @Nested
    @DisplayName("GeneratedSessionFeedback 예시")
    class SessionFeedbackExample {

        @Test
        @DisplayName("루트 키 집합이 schema required 와 일치한다")
        void rootKeys_match_schema_required() throws Exception {
            JsonNode root = parse(GeneratedSessionFeedback.class);

            assertThat(fieldNames(root)).isEqualTo(SESSION_FEEDBACK_ROOT_KEYS);
        }

        @Test
        @DisplayName("overall 객체 키 집합이 schema required 와 일치한다")
        void overallKeys_match_schema_required() throws Exception {
            JsonNode root = parse(GeneratedSessionFeedback.class);

            assertThat(fieldNames(root.get("overall"))).isEqualTo(SESSION_FEEDBACK_OVERALL_KEYS);
        }

        @Test
        @DisplayName("overall.dimension_scores 키가 한국어 14개 라벨과 일치한다")
        void dimensionScores_keys_match_dimension_keys() throws Exception {
            JsonNode root = parse(GeneratedSessionFeedback.class);

            assertThat(fieldNames(root.get("overall").get("dimension_scores")))
                    .isEqualTo(new LinkedHashSet<>(SESSION_FEEDBACK_DIMENSION_KEYS));
        }

        @Test
        @DisplayName("strengths / gaps dimension 라벨이 14개 한국어 키 중 하나다")
        void strengths_gaps_dimension_labels_are_korean_keys() throws Exception {
            JsonNode root = parse(GeneratedSessionFeedback.class);

            for (JsonNode strength : root.get("strengths")) {
                assertThat(SESSION_FEEDBACK_DIMENSION_KEYS).contains(strength.get("dimension").asText());
            }
            for (JsonNode gap : root.get("gaps")) {
                assertThat(SESSION_FEEDBACK_DIMENSION_KEYS).contains(gap.get("dimension").asText());
            }
        }
    }

    private JsonNode parse(Class<?> clazz) throws Exception {
        String example = registry.exampleFor(clazz);
        assertThat(example).as("registry must contain example for " + clazz.getSimpleName()).isNotNull();
        return objectMapper.readTree(example);
    }

    private Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }
}
