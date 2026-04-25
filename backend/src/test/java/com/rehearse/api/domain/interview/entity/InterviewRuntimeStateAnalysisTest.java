package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InterviewRuntimeState - turnAnalysisCache 접근자")
class InterviewRuntimeStateAnalysisTest {

    private InterviewRuntimeState state;

    @BeforeEach
    void setUp() {
        CachedResumeSkeleton skeleton = () -> "hash";
        state = new InterviewRuntimeState("MID", skeleton);
    }

    private AnswerAnalysis sampleAnalysis(long turnId) {
        return new AnswerAnalysis(
                turnId,
                List.of(new Claim("c", 3, EvidenceStrength.WEAK, "tag")),
                List.of(),
                List.of(),
                3,
                RecommendedNextAction.DEEP_DIVE
        );
    }

    @Test
    @DisplayName("getAnswerAnalysis_returns_empty_when_turn_not_recorded")
    void getAnswerAnalysis_returns_empty_when_turn_not_recorded() {
        assertThat(state.getAnswerAnalysis(1L)).isEmpty();
    }

    @Test
    @DisplayName("recordAnalysis_then_getAnswerAnalysis_returns_recorded_record")
    void recordAnalysis_then_getAnswerAnalysis_returns_recorded_record() {
        AnswerAnalysis analysis = sampleAnalysis(10L);

        state.recordAnalysis(10L, analysis);

        Optional<AnswerAnalysis> cached = state.getAnswerAnalysis(10L);
        assertThat(cached).isPresent();
        assertThat(cached.get()).isEqualTo(analysis);
    }

    @Test
    @DisplayName("recordAnalysis_overwrites_previous_entry_for_same_turn")
    void recordAnalysis_overwrites_previous_entry_for_same_turn() {
        state.recordAnalysis(1L, sampleAnalysis(1L));
        AnswerAnalysis updated = sampleAnalysis(1L).withRecommendedNextAction(RecommendedNextAction.SKIP);

        state.recordAnalysis(1L, updated);

        assertThat(state.getAnswerAnalysis(1L).get().recommendedNextAction())
                .isEqualTo(RecommendedNextAction.SKIP);
    }

    @Test
    @DisplayName("getAnswerAnalysis_returns_empty_when_cached_is_other_TurnAnalysis_subtype")
    void getAnswerAnalysis_returns_empty_when_cached_is_other_TurnAnalysis_subtype() {
        TurnAnalysis nonAnswer = () -> 99L;

        state.recordAnalysis(99L, nonAnswer);

        assertThat(state.getAnswerAnalysis(99L)).isEmpty();
    }

    @Test
    @DisplayName("recordAnalysis_throws_when_turnId_or_analysis_is_null")
    void recordAnalysis_throws_when_turnId_or_analysis_is_null() {
        assertThatThrownBy(() -> state.recordAnalysis(null, sampleAnalysis(1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> state.recordAnalysis(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getAnswerAnalysis_returns_empty_when_turnId_is_null")
    void getAnswerAnalysis_returns_empty_when_turnId_is_null() {
        assertThat(state.getAnswerAnalysis(null)).isEmpty();
    }
}
