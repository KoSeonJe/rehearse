package com.rehearse.api.domain.questionset.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertStatusTest {

    // ─────────────────────────────────────────────────────────────
    // canTransitionTo — PENDING
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING → PROCESSING: 허용된다")
    void pending_canTransitionTo_processing() {
        assertThat(ConvertStatus.PENDING.canTransitionTo(ConvertStatus.PROCESSING)).isTrue();
    }

    @Test
    @DisplayName("PENDING → FAILED: 허용된다")
    void pending_canTransitionTo_failed() {
        assertThat(ConvertStatus.PENDING.canTransitionTo(ConvertStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("PENDING → COMPLETED: 허용되지 않는다")
    void pending_cannotTransitionTo_completed() {
        assertThat(ConvertStatus.PENDING.canTransitionTo(ConvertStatus.COMPLETED)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // canTransitionTo — PROCESSING
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PROCESSING → COMPLETED: 허용된다")
    void processing_canTransitionTo_completed() {
        assertThat(ConvertStatus.PROCESSING.canTransitionTo(ConvertStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("PROCESSING → FAILED: 허용된다")
    void processing_canTransitionTo_failed() {
        assertThat(ConvertStatus.PROCESSING.canTransitionTo(ConvertStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("PROCESSING → PENDING: 허용되지 않는다")
    void processing_cannotTransitionTo_pending() {
        assertThat(ConvertStatus.PROCESSING.canTransitionTo(ConvertStatus.PENDING)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // canTransitionTo — COMPLETED
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("COMPLETED → 모든 상태: 허용되지 않는다")
    void completed_cannotTransitionToAnyStatus() {
        for (ConvertStatus target : ConvertStatus.values()) {
            assertThat(ConvertStatus.COMPLETED.canTransitionTo(target))
                    .as("COMPLETED → %s 는 허용되지 않아야 한다", target)
                    .isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // canTransitionTo — FAILED
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FAILED → PROCESSING: 허용된다")
    void failed_canTransitionTo_processing() {
        assertThat(ConvertStatus.FAILED.canTransitionTo(ConvertStatus.PROCESSING)).isTrue();
    }

    @Test
    @DisplayName("FAILED → COMPLETED: 허용되지 않는다")
    void failed_cannotTransitionTo_completed() {
        assertThat(ConvertStatus.FAILED.canTransitionTo(ConvertStatus.COMPLETED)).isFalse();
    }
}
