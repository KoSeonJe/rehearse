package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerAnalysisTest {

    @Test
    void empty_returns_clarification_default() {
        AnswerAnalysis empty = AnswerAnalysis.empty();
        assertThat(empty.transcript()).isEmpty();
        assertThat(empty.claims()).isEmpty();
        assertThat(empty.dimensionGaps()).isEmpty();
        assertThat(empty.unstatedAssumptions()).isEmpty();
        assertThat(empty.weakestDimension()).isNull();
        assertThat(empty.recommendedNextAction()).isEqualTo(RecommendedNextAction.CLARIFICATION);
    }

    @Test
    void null_collections_normalize_to_empty() {
        AnswerAnalysis a = new AnswerAnalysis("전사 텍스트", null, null, "depth", null, RecommendedNextAction.DEEP_DIVE);
        assertThat(a.claims()).isEmpty();
        assertThat(a.dimensionGaps()).isEmpty();
        assertThat(a.unstatedAssumptions()).isEmpty();
    }

    @Test
    void null_transcript_normalizes_to_empty_string() {
        AnswerAnalysis a = new AnswerAnalysis(null, List.of(), Map.of(), null, List.of(), RecommendedNextAction.CLARIFICATION);
        assertThat(a.transcript()).isEmpty();
    }

    @Test
    void collections_are_immutable_copies() {
        Map<String, Integer> gaps = new java.util.HashMap<>();
        gaps.put("clarity", 1);
        AnswerAnalysis a = new AnswerAnalysis("전사", List.of(), gaps, "clarity", List.of(), RecommendedNextAction.DEEP_DIVE);
        assertThatThrownBy(() -> a.dimensionGaps().put("depth", 2))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
