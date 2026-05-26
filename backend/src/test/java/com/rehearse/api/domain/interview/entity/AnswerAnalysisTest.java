package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnswerAnalysisTest {

    @Test
    void empty_returns_clarification_default() {
        AnswerAnalysis empty = AnswerAnalysis.empty();
        assertThat(empty.claims()).isEmpty();
        assertThat(empty.dimensionGaps()).isEmpty();
        assertThat(empty.unstatedAssumptions()).isEmpty();
        assertThat(empty.weakestDimension()).isNull();
        assertThat(empty.recommendedNextAction()).isEqualTo(RecommendedNextAction.CLARIFICATION);
    }

    @Test
    @DisplayName("empty() 의 transcript 는 빈 문자열이다")
    void empty_returns_blank_transcript() {
        assertThat(AnswerAnalysis.empty().transcript()).isEqualTo("");
    }

    @Test
    @DisplayName("transcript null 은 빈 문자열로 정규화된다")
    void null_transcript_normalizes_to_blank() {
        AnswerAnalysis a = new AnswerAnalysis(null, null, null, "depth", null, RecommendedNextAction.DEEP_DIVE);
        assertThat(a.transcript()).isEqualTo("");
    }

    @Test
    @DisplayName("transcript 값은 그대로 보존된다")
    void transcript_value_preserved() {
        AnswerAnalysis a = new AnswerAnalysis("정상 전사 텍스트", null, null, "depth", null, RecommendedNextAction.DEEP_DIVE);
        assertThat(a.transcript()).isEqualTo("정상 전사 텍스트");
    }

    @Test
    void null_collections_normalize_to_empty() {
        AnswerAnalysis a = new AnswerAnalysis(null, null, null, "depth", null, RecommendedNextAction.DEEP_DIVE);
        assertThat(a.claims()).isEmpty();
        assertThat(a.dimensionGaps()).isEmpty();
        assertThat(a.unstatedAssumptions()).isEmpty();
    }

    @Test
    void collections_are_immutable_copies() {
        Map<String, Integer> gaps = new java.util.HashMap<>();
        gaps.put("clarity", 1);
        AnswerAnalysis a = new AnswerAnalysis("text", List.of(), gaps, "clarity", List.of(), RecommendedNextAction.DEEP_DIVE);
        assertThatThrownBy(() -> a.dimensionGaps().put("depth", 2))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
