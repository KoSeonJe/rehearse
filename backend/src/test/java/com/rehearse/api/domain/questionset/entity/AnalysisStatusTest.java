package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.analysis.entity.AnalysisStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisStatusTest {

    @Nested
    @DisplayName("PENDING 상태 전이")
    class PendingTransition {

        @Test
        @DisplayName("PENDING → PENDING_UPLOAD: 허용된다")
        void pending_canTransitionTo_pendingUpload() {
            // when & then
            assertThat(AnalysisStatus.PENDING.canTransitionTo(AnalysisStatus.PENDING_UPLOAD)).isTrue();
        }

        @Test
        @DisplayName("PENDING → SKIPPED: 허용된다")
        void pending_canTransitionTo_skipped() {
            // when & then
            assertThat(AnalysisStatus.PENDING.canTransitionTo(AnalysisStatus.SKIPPED)).isTrue();
        }

        @Test
        @DisplayName("PENDING → FAILED: 허용된다")
        void pending_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.PENDING.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("PENDING → COMPLETED: 허용되지 않는다")
        void pending_cannotTransitionTo_completed() {
            // when & then
            assertThat(AnalysisStatus.PENDING.canTransitionTo(AnalysisStatus.COMPLETED)).isFalse();
        }
    }

    @Nested
    @DisplayName("PENDING_UPLOAD 상태 전이")
    class PendingUploadTransition {

        @Test
        @DisplayName("PENDING_UPLOAD → EXTRACTING: 허용된다")
        void pendingUpload_canTransitionTo_extracting() {
            // when & then
            assertThat(AnalysisStatus.PENDING_UPLOAD.canTransitionTo(AnalysisStatus.EXTRACTING)).isTrue();
        }

        @Test
        @DisplayName("PENDING_UPLOAD → FAILED: 허용된다")
        void pendingUpload_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.PENDING_UPLOAD.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("PENDING_UPLOAD → ANALYZING: 허용되지 않는다")
        void pendingUpload_cannotTransitionTo_analyzing() {
            // when & then
            assertThat(AnalysisStatus.PENDING_UPLOAD.canTransitionTo(AnalysisStatus.ANALYZING)).isFalse();
        }
    }

    @Nested
    @DisplayName("EXTRACTING 상태 전이")
    class ExtractingTransition {

        @Test
        @DisplayName("EXTRACTING → ANALYZING: 허용된다")
        void extracting_canTransitionTo_analyzing() {
            // when & then
            assertThat(AnalysisStatus.EXTRACTING.canTransitionTo(AnalysisStatus.ANALYZING)).isTrue();
        }

        @Test
        @DisplayName("EXTRACTING → FAILED: 허용된다")
        void extracting_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.EXTRACTING.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("EXTRACTING → COMPLETED: 허용되지 않는다")
        void extracting_cannotTransitionTo_completed() {
            // when & then
            assertThat(AnalysisStatus.EXTRACTING.canTransitionTo(AnalysisStatus.COMPLETED)).isFalse();
        }
    }

    @Nested
    @DisplayName("ANALYZING 상태 전이")
    class AnalyzingTransition {

        @Test
        @DisplayName("ANALYZING → FINALIZING: 허용된다")
        void analyzing_canTransitionTo_finalizing() {
            // when & then
            assertThat(AnalysisStatus.ANALYZING.canTransitionTo(AnalysisStatus.FINALIZING)).isTrue();
        }

        @Test
        @DisplayName("ANALYZING → FAILED: 허용된다")
        void analyzing_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.ANALYZING.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("ANALYZING → COMPLETED: 허용되지 않는다")
        void analyzing_cannotTransitionTo_completed() {
            // when & then
            assertThat(AnalysisStatus.ANALYZING.canTransitionTo(AnalysisStatus.COMPLETED)).isFalse();
        }
    }

    @Nested
    @DisplayName("FINALIZING 상태 전이")
    class FinalizingTransition {

        @Test
        @DisplayName("FINALIZING → COMPLETED: 허용된다")
        void finalizing_canTransitionTo_completed() {
            // when & then
            assertThat(AnalysisStatus.FINALIZING.canTransitionTo(AnalysisStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("FINALIZING → PARTIAL: 허용된다")
        void finalizing_canTransitionTo_partial() {
            // when & then
            assertThat(AnalysisStatus.FINALIZING.canTransitionTo(AnalysisStatus.PARTIAL)).isTrue();
        }

        @Test
        @DisplayName("FINALIZING → FAILED: 허용된다")
        void finalizing_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.FINALIZING.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("FINALIZING → ANALYZING: 허용되지 않는다")
        void finalizing_cannotTransitionTo_analyzing() {
            // when & then
            assertThat(AnalysisStatus.FINALIZING.canTransitionTo(AnalysisStatus.ANALYZING)).isFalse();
        }
    }

    @Nested
    @DisplayName("COMPLETED 상태 전이")
    class CompletedTransition {

        @Test
        @DisplayName("COMPLETED → FAILED: 허용된다")
        void completed_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.COMPLETED.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("COMPLETED → EXTRACTING: 허용되지 않는다")
        void completed_cannotTransitionTo_extracting() {
            // when & then
            assertThat(AnalysisStatus.COMPLETED.canTransitionTo(AnalysisStatus.EXTRACTING)).isFalse();
        }
    }

    @Nested
    @DisplayName("PARTIAL 상태 전이")
    class PartialTransition {

        @Test
        @DisplayName("PARTIAL → EXTRACTING: 허용된다")
        void partial_canTransitionTo_extracting() {
            // when & then
            assertThat(AnalysisStatus.PARTIAL.canTransitionTo(AnalysisStatus.EXTRACTING)).isTrue();
        }

        @Test
        @DisplayName("PARTIAL → FAILED: 허용된다")
        void partial_canTransitionTo_failed() {
            // when & then
            assertThat(AnalysisStatus.PARTIAL.canTransitionTo(AnalysisStatus.FAILED)).isTrue();
        }

        @Test
        @DisplayName("PARTIAL → COMPLETED: 허용된다 (재분석 없이 추가 데이터로 보정)")
        void partial_canTransitionTo_completed() {
            // when & then
            assertThat(AnalysisStatus.PARTIAL.canTransitionTo(AnalysisStatus.COMPLETED)).isTrue();
        }
    }

    @Nested
    @DisplayName("FAILED 상태 전이")
    class FailedTransition {

        @Test
        @DisplayName("FAILED → EXTRACTING: 허용된다")
        void failed_canTransitionTo_extracting() {
            // when & then
            assertThat(AnalysisStatus.FAILED.canTransitionTo(AnalysisStatus.EXTRACTING)).isTrue();
        }

        @Test
        @DisplayName("FAILED → COMPLETED: 허용된다 (좀비 스케줄러 FAILED 처리 후 Lambda 뒤늦은 성공)")
        void failed_canTransitionTo_completed() {
            // when & then
            assertThat(AnalysisStatus.FAILED.canTransitionTo(AnalysisStatus.COMPLETED)).isTrue();
        }
    }

    @Nested
    @DisplayName("SKIPPED 상태 전이")
    class SkippedTransition {

        @Test
        @DisplayName("SKIPPED → 모든 상태: 허용되지 않는다")
        void skipped_cannotTransitionToAnyStatus() {
            // when & then
            for (AnalysisStatus target : AnalysisStatus.values()) {
                assertThat(AnalysisStatus.SKIPPED.canTransitionTo(target))
                        .as("SKIPPED → %s 는 허용되지 않아야 한다", target)
                        .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("isResolved 메서드")
    class IsResolved {

        @Test
        @DisplayName("isResolved: COMPLETED는 true를 반환한다")
        void isResolved_completed_returnsTrue() {
            // when & then
            assertThat(AnalysisStatus.COMPLETED.isResolved()).isTrue();
        }

        @Test
        @DisplayName("isResolved: PARTIAL은 true를 반환한다")
        void isResolved_partial_returnsTrue() {
            // when & then
            assertThat(AnalysisStatus.PARTIAL.isResolved()).isTrue();
        }

        @Test
        @DisplayName("isResolved: SKIPPED는 true를 반환한다")
        void isResolved_skipped_returnsTrue() {
            // when & then
            assertThat(AnalysisStatus.SKIPPED.isResolved()).isTrue();
        }

        @Test
        @DisplayName("isResolved: FAILED는 false를 반환한다")
        void isResolved_failed_returnsFalse() {
            // when & then
            assertThat(AnalysisStatus.FAILED.isResolved()).isFalse();
        }

        @Test
        @DisplayName("isResolved: PENDING은 false를 반환한다")
        void isResolved_pending_returnsFalse() {
            // when & then
            assertThat(AnalysisStatus.PENDING.isResolved()).isFalse();
        }
    }
}
