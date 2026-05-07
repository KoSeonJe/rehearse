package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rubric.selectDimensions")
class RubricTest {

    private static final List<DimensionRef> ALL_DIMS = List.of(
            new DimensionRef("technical_depth", 0.25),
            new DimensionRef("reasoning_communication", 0.15),
            new DimensionRef("experience_concreteness", 0.20),
            new DimensionRef("factual_consistency", 0.20),
            new DimensionRef("chain_depth", 0.20)
    );

    @Nested
    @DisplayName("Resume Track mode 매핑")
    class ResumeModeMapping {

        @Test
        @DisplayName("PLAYGROUND mode → on_playground_mode 차원 반환")
        void playground_mode_returns_correct_dims() {
            Rubric rubric = rubricWith(Map.of(
                    "on_playground_mode", List.of("experience_concreteness")
            ));

            List<String> result = rubric.selectDimensions(ResumeMode.PLAYGROUND);

            assertThat(result).containsExactly("experience_concreteness");
        }

        @Test
        @DisplayName("INTERROGATION mode → on_interrogation_mode 차원 반환")
        void interrogation_mode_returns_correct_dims() {
            Rubric rubric = rubricWith(Map.of(
                    "on_interrogation_mode", List.of("technical_depth", "reasoning_communication", "factual_consistency", "chain_depth")
            ));

            List<String> result = rubric.selectDimensions(ResumeMode.INTERROGATION);

            assertThat(result).containsExactlyInAnyOrder("technical_depth", "reasoning_communication", "factual_consistency", "chain_depth");
        }

        @Test
        @DisplayName("resumeMode에 해당 키 없으면 on_intent_answer fallback")
        void resume_mode_missing_key_falls_back_to_intent_answer() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_answer", List.of("technical_depth", "reasoning_communication")
            ));

            List<String> result = rubric.selectDimensions(ResumeMode.PLAYGROUND);

            assertThat(result).containsExactly("technical_depth", "reasoning_communication");
        }
    }

    @Nested
    @DisplayName("on_intent_answer fallback")
    class AnswerIntentFallback {

        @Test
        @DisplayName("resumeMode null → on_intent_answer 반환")
        void answer_no_mode_uses_intent_answer() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_answer", List.of("technical_depth", "reasoning_communication", "conceptual_accuracy")
            ));

            List<String> result = rubric.selectDimensions(null);

            assertThat(result).containsExactly("technical_depth", "reasoning_communication", "conceptual_accuracy");
        }

        @Test
        @DisplayName("perTurnRules 비어있으면 usesDimensions 전체 반환")
        void empty_per_turn_rules_returns_all_uses_dimensions() {
            Rubric rubric = rubricWith(Map.of());

            List<String> result = rubric.selectDimensions(null);

            assertThat(result).containsExactlyInAnyOrder("technical_depth", "reasoning_communication", "experience_concreteness", "factual_consistency", "chain_depth");
        }
    }

    private Rubric rubricWith(Map<String, List<String>> perTurnRules) {
        return new Rubric("test-rubric-v1", "테스트 루브릭", ALL_DIMS, perTurnRules, Map.of());
    }
}
