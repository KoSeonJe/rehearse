package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewRuntimeState - compaction 요약 및 in-flight 관리")
class InterviewRuntimeStateCompactionTest {

    private InterviewRuntimeState state;

    @BeforeEach
    void setUp() {
        CachedResumeSkeleton skeleton = () -> "hash";
        state = new InterviewRuntimeState("MID", skeleton);
    }

    @Test
    @DisplayName("summary_absent_when_no_compaction_stored")
    void summary_absent_when_no_compaction_stored() {
        Optional<String> result = state.getCompactedSummary(3);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("summary_roundtrip_when_put_then_get")
    void summary_roundtrip_when_put_then_get() {
        state.putCompactedSummary(3, "topics: GC / claims: STW 발생");

        Optional<String> result = state.getCompactedSummary(3);

        assertThat(result).isPresent();
        assertThat(result.get()).contains("GC");
    }

    @Test
    @DisplayName("different_window_ends_are_independent_keys")
    void different_window_ends_are_independent_keys() {
        state.putCompactedSummary(3, "summary-for-3");
        state.putCompactedSummary(7, "summary-for-7");

        assertThat(state.getCompactedSummary(3).orElseThrow()).isEqualTo("summary-for-3");
        assertThat(state.getCompactedSummary(7).orElseThrow()).isEqualTo("summary-for-7");
        assertThat(state.getCompactedSummary(5)).isEmpty();
    }

    @Test
    @DisplayName("in_flight_false_initially")
    void in_flight_false_initially() {
        assertThat(state.hasCompactionInFlight(3)).isFalse();
    }

    @Test
    @DisplayName("in_flight_true_after_mark_started")
    void in_flight_true_after_mark_started() {
        state.markCompactionStarted(3);

        assertThat(state.hasCompactionInFlight(3)).isTrue();
    }

    @Test
    @DisplayName("in_flight_false_after_mark_finished")
    void in_flight_false_after_mark_finished() {
        state.markCompactionStarted(3);
        state.markCompactionFinished(3);

        assertThat(state.hasCompactionInFlight(3)).isFalse();
    }

    @Test
    @DisplayName("in_flight_lifecycle_is_per_window_end")
    void in_flight_lifecycle_is_per_window_end() {
        state.markCompactionStarted(3);
        state.markCompactionStarted(7);
        state.markCompactionFinished(3);

        assertThat(state.hasCompactionInFlight(3)).isFalse();
        assertThat(state.hasCompactionInFlight(7)).isTrue();
    }
}
