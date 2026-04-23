package com.rehearse.api.domain.questionset.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertStatusTest {

    @Nested
    @DisplayName("PENDING 상태 전이")
    class PendingTransition {

        @Test
        @DisplayName("PENDING → PROCESSING: 허용된다")
        void pending_canTransitionTo_processing() {
            // when & then
            assertThat(ConvertStatus.PENDING.canTransitionTo(ConvertStatus.PROCESSING)).isTrue();
        }

        @Test
        @DisplayName("PENDING → FAILED: 허용된다")
        void pending_canTransitionTo_failed() {
            // when & then
            assertThat(ConvertStatus.PENDING.canTransitionTo(ConvertStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("PENDING → COMPLETED: 허용되지 않는다")
        void pending_cannotTransitionTo_completed() {
            // when & then
            assertThat(ConvertStatus.PENDING.canTransitionTo(ConvertStatus.COMPLETED)).isFalse();
        }
    }

    @Nested
    @DisplayName("PROCESSING 상태 전이")
    class ProcessingTransition {

        @Test
        @DisplayName("PROCESSING → COMPLETED: 허용된다")
        void processing_canTransitionTo_completed() {
            // when & then
            assertThat(ConvertStatus.PROCESSING.canTransitionTo(ConvertStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("PROCESSING → FAILED: 허용된다")
        void processing_canTransitionTo_failed() {
            // when & then
            assertThat(ConvertStatus.PROCESSING.canTransitionTo(ConvertStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("PROCESSING → PENDING: 허용되지 않는다")
        void processing_cannotTransitionTo_pending() {
            // when & then
            assertThat(ConvertStatus.PROCESSING.canTransitionTo(ConvertStatus.PENDING)).isFalse();
        }
    }

    @Nested
    @DisplayName("COMPLETED 상태 전이")
    class CompletedTransition {

        @Test
        @DisplayName("COMPLETED → FAILED: 허용된다 (변환 후 무결성 검사 실패)")
        void completed_canTransitionTo_failed() {
            // when & then
            assertThat(ConvertStatus.COMPLETED.canTransitionTo(ConvertStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("COMPLETED → FAILED 외 상태: 허용되지 않는다")
        void completed_cannotTransitionToOtherStatus() {
            // when & then
            for (ConvertStatus target : ConvertStatus.values()) {
                if (target == ConvertStatus.FAILED) continue;
                assertThat(ConvertStatus.COMPLETED.canTransitionTo(target))
                        .as("COMPLETED → %s 는 허용되지 않아야 한다", target)
                        .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("FAILED 상태 전이")
    class FailedTransition {

        @Test
        @DisplayName("FAILED → PROCESSING: 허용된다")
        void failed_canTransitionTo_processing() {
            // when & then
            assertThat(ConvertStatus.FAILED.canTransitionTo(ConvertStatus.PROCESSING)).isTrue();
        }

        @Test
        @DisplayName("FAILED → COMPLETED: 허용되지 않는다")
        void failed_cannotTransitionTo_completed() {
            // when & then
            assertThat(ConvertStatus.FAILED.canTransitionTo(ConvertStatus.COMPLETED)).isFalse();
        }
    }
}
