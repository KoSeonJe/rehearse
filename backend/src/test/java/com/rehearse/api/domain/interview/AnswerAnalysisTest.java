package com.rehearse.api.domain.interview;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AnswerAnalysis / Claim record - 컴팩트 생성자 검증")
class AnswerAnalysisTest {

    @Test
    @DisplayName("claim_rejects_depth_score_outside_1_to_5")
    void claim_rejects_depth_score_outside_1_to_5() {
        assertThatThrownBy(() -> new Claim("t", 0, EvidenceStrength.WEAK, "tag"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Claim("t", 6, EvidenceStrength.WEAK, "tag"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("claim_accepts_depth_score_at_boundaries")
    void claim_accepts_depth_score_at_boundaries() {
        assertThat(new Claim("t", 1, EvidenceStrength.WEAK, "tag").depthScore()).isEqualTo(1);
        assertThat(new Claim("t", 5, EvidenceStrength.STRONG, "tag").depthScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("claim_rejects_blank_text")
    void claim_rejects_blank_text() {
        assertThatThrownBy(() -> new Claim("", 3, EvidenceStrength.WEAK, "tag"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Claim("  ", 3, EvidenceStrength.WEAK, "tag"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("answer_analysis_rejects_quality_outside_1_to_5")
    void answer_analysis_rejects_quality_outside_1_to_5() {
        assertThatThrownBy(() -> new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 0, RecommendedNextAction.SKIP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 6, RecommendedNextAction.SKIP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("answer_analysis_normalizes_null_lists_to_empty")
    void answer_analysis_normalizes_null_lists_to_empty() {
        AnswerAnalysis a = new AnswerAnalysis(1L, null, null, null, 3, RecommendedNextAction.DEEP_DIVE);

        assertThat(a.claims()).isEmpty();
        assertThat(a.missingPerspectives()).isEmpty();
        assertThat(a.unstatedAssumptions()).isEmpty();
    }

    @Test
    @DisplayName("with_recommended_next_action_returns_new_instance")
    void with_recommended_next_action_returns_new_instance() {
        AnswerAnalysis original = new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 1, RecommendedNextAction.DEEP_DIVE);

        AnswerAnalysis updated = original.withRecommendedNextAction(RecommendedNextAction.CLARIFICATION);

        assertThat(updated.recommendedNextAction()).isEqualTo(RecommendedNextAction.CLARIFICATION);
        assertThat(original.recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
    }

    @Test
    @DisplayName("with_turn_id_returns_new_instance_with_other_fields_preserved")
    void with_turn_id_returns_new_instance_with_other_fields_preserved() {
        AnswerAnalysis original = new AnswerAnalysis(0L, List.of(), List.of(Perspective.TRADEOFF), List.of(), 4, RecommendedNextAction.SKIP);

        AnswerAnalysis updated = original.withTurnId(42L);

        assertThat(updated.turnId()).isEqualTo(42L);
        assertThat(updated.missingPerspectives()).containsExactly(Perspective.TRADEOFF);
        assertThat(updated.answerQuality()).isEqualTo(4);
    }
}
