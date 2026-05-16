package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
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
    @DisplayName("on_intent_answer 우선")
    class IntentAnswerPriority {

        @Test
        @DisplayName("on_intent_answer 정의 있으면 해당 차원 반환")
        void uses_intent_answer_when_defined() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_answer", List.of("technical_depth", "reasoning_communication", "conceptual_accuracy")
            ));

            List<String> result = rubric.selectDimensions();

            assertThat(result).containsExactly("technical_depth", "reasoning_communication", "conceptual_accuracy");
        }
    }

    @Nested
    @DisplayName("usesDimensions fallback")
    class UsesDimensionsFallback {

        @Test
        @DisplayName("perTurnRules 비어있으면 usesDimensions 전체 반환")
        void empty_per_turn_rules_returns_all_uses_dimensions() {
            Rubric rubric = rubricWith(Map.of());

            List<String> result = rubric.selectDimensions();

            assertThat(result).containsExactlyInAnyOrder("technical_depth", "reasoning_communication", "experience_concreteness", "factual_consistency", "chain_depth");
        }
    }

    private Rubric rubricWith(Map<String, List<String>> perTurnRules) {
        return new Rubric("test-rubric-v1", "테스트 루브릭", ALL_DIMS, perTurnRules, Map.of());
    }
}
