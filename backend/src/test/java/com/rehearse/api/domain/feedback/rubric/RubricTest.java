package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.resume.domain.ResumeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rubric.selectDimensions")
class RubricTest {

    private static final List<DimensionRef> ALL_DIMS = List.of(
            new DimensionRef("D2", 0.25),
            new DimensionRef("D3", 0.15),
            new DimensionRef("D6", 0.20),
            new DimensionRef("D9", 0.20),
            new DimensionRef("D10", 0.20)
    );

    @Nested
    @DisplayName("on_intent_clarify")
    class ClarifyIntent {

        @Test
        @DisplayName("CLARIFY_REQUEST intent → empty 반환")
        void clarify_returns_empty() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_clarify", List.of()
            ));

            List<String> result = rubric.selectDimensions(IntentType.CLARIFY_REQUEST, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("CLARIFY_REQUEST intent — perTurnRules에 키 없으면 empty 반환")
        void clarify_missing_key_returns_empty() {
            Rubric rubric = rubricWith(Map.of());

            List<String> result = rubric.selectDimensions(IntentType.CLARIFY_REQUEST, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("on_intent_give_up")
    class GiveUpIntent {

        @Test
        @DisplayName("GIVE_UP intent → 해당 차원만 반환")
        void give_up_returns_only_d8() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_give_up", List.of("D8")
            ));

            List<String> result = rubric.selectDimensions(IntentType.GIVE_UP, null);

            assertThat(result).containsExactly("D8");
        }

        @Test
        @DisplayName("GIVE_UP intent — perTurnRules에 키 없으면 empty 반환")
        void give_up_missing_key_returns_empty() {
            Rubric rubric = rubricWith(Map.of());

            List<String> result = rubric.selectDimensions(IntentType.GIVE_UP, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Resume Track mode 매핑")
    class ResumeModeMapping {

        @Test
        @DisplayName("PLAYGROUND mode → on_playground_mode 차원 반환")
        void playground_mode_returns_correct_dims() {
            Rubric rubric = rubricWith(Map.of(
                    "on_playground_mode", List.of("D6")
            ));

            List<String> result = rubric.selectDimensions(IntentType.ANSWER, ResumeMode.PLAYGROUND);

            assertThat(result).containsExactly("D6");
        }

        @Test
        @DisplayName("INTERROGATION mode → on_interrogation_mode 차원 반환")
        void interrogation_mode_returns_correct_dims() {
            Rubric rubric = rubricWith(Map.of(
                    "on_interrogation_mode", List.of("D2", "D3", "D9", "D10")
            ));

            List<String> result = rubric.selectDimensions(IntentType.ANSWER, ResumeMode.INTERROGATION);

            assertThat(result).containsExactlyInAnyOrder("D2", "D3", "D9", "D10");
        }

        @Test
        @DisplayName("WRAP_UP mode → on_wrap_up_mode 차원 반환")
        void wrap_up_mode_returns_correct_dims() {
            Rubric rubric = rubricWith(Map.of(
                    "on_wrap_up_mode", List.of("D10")
            ));

            List<String> result = rubric.selectDimensions(IntentType.ANSWER, ResumeMode.WRAP_UP);

            assertThat(result).containsExactly("D10");
        }

        @Test
        @DisplayName("resumeMode에 해당 키 없으면 on_intent_answer fallback")
        void resume_mode_missing_key_falls_back_to_intent_answer() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_answer", List.of("D2", "D3")
            ));

            List<String> result = rubric.selectDimensions(IntentType.ANSWER, ResumeMode.PLAYGROUND);

            assertThat(result).containsExactly("D2", "D3");
        }
    }

    @Nested
    @DisplayName("on_intent_answer fallback")
    class AnswerIntentFallback {

        @Test
        @DisplayName("ANSWER intent, resumeMode null → on_intent_answer 반환")
        void answer_no_mode_uses_intent_answer() {
            Rubric rubric = rubricWith(Map.of(
                    "on_intent_answer", List.of("D2", "D3", "D4")
            ));

            List<String> result = rubric.selectDimensions(IntentType.ANSWER, null);

            assertThat(result).containsExactly("D2", "D3", "D4");
        }

        @Test
        @DisplayName("perTurnRules 비어있고 intent=ANSWER → usesDimensions 전체 반환")
        void empty_per_turn_rules_returns_all_uses_dimensions() {
            Rubric rubric = rubricWith(Map.of());

            List<String> result = rubric.selectDimensions(IntentType.ANSWER, null);

            assertThat(result).containsExactlyInAnyOrder("D2", "D3", "D6", "D9", "D10");
        }
    }

    private Rubric rubricWith(Map<String, List<String>> perTurnRules) {
        return new Rubric("test-rubric-v1", "테스트 루브릭", ALL_DIMS, perTurnRules, Map.of());
    }
}
