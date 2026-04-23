package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionSetAnalysisTest {

    @Nested
    @DisplayName("completeAnalysis 메서드")
    class CompleteAnalysis {

        @Test
        @DisplayName("completeAnalysis(true, true) → COMPLETED")
        void completeAnalysis_bothTrue_returnsCompleted() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

            // when
            analysis.completeAnalysis(true, true);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
            assertThat(analysis.isVerbalCompleted()).isTrue();
            assertThat(analysis.isNonverbalCompleted()).isTrue();
        }

        @Test
        @DisplayName("completeAnalysis(true, false) → PARTIAL")
        void completeAnalysis_verbalOnly_returnsPartial() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

            // when
            analysis.completeAnalysis(true, false);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
            assertThat(analysis.isVerbalCompleted()).isTrue();
            assertThat(analysis.isNonverbalCompleted()).isFalse();
        }

        @Test
        @DisplayName("completeAnalysis(false, true) → PARTIAL")
        void completeAnalysis_nonverbalOnly_returnsPartial() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

            // when
            analysis.completeAnalysis(false, true);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
            assertThat(analysis.isVerbalCompleted()).isFalse();
            assertThat(analysis.isNonverbalCompleted()).isTrue();
        }

        @Test
        @DisplayName("completeAnalysis(false, false) → FAILED")
        void completeAnalysis_bothFalse_returnsFailed() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

            // when
            analysis.completeAnalysis(false, false);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(analysis.isVerbalCompleted()).isFalse();
            assertThat(analysis.isNonverbalCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("isFullyReady 메서드")
    class IsFullyReady {

        @Test
        @DisplayName("isFullyReady: analysisStatus=COMPLETED, convertStatus=COMPLETED → true")
        void isFullyReady_completedAndConverted_returnsTrue() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.COMPLETED);
            ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.COMPLETED);

            // when & then
            assertThat(analysis.isFullyReady()).isTrue();
        }

        @Test
        @DisplayName("isFullyReady: analysisStatus=PARTIAL, convertStatus=COMPLETED → true")
        void isFullyReady_partialAndConverted_returnsTrue() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PARTIAL);
            ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.COMPLETED);

            // when & then
            assertThat(analysis.isFullyReady()).isTrue();
        }

        @Test
        @DisplayName("isFullyReady: analysisStatus=COMPLETED, convertStatus=PENDING → false")
        void isFullyReady_completedButConvertPending_returnsFalse() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.COMPLETED);
            // convertStatus 기본값은 PENDING

            // when & then
            assertThat(analysis.isFullyReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("markFailed 메서드")
    class MarkFailed {

        @Test
        @DisplayName("markFailed: failureReason과 failureDetail이 설정되고 FAILED 상태로 전이된다")
        void markFailed_setsReasonAndDetail() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.ANALYZING);

            // when
            analysis.markFailed("ZOMBIE_TIMEOUT", "분석이 10분 내 완료되지 않음");

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(analysis.getFailureReason()).isEqualTo("ZOMBIE_TIMEOUT");
            assertThat(analysis.getFailureDetail()).isEqualTo("분석이 10분 내 완료되지 않음");
        }
    }

    @Nested
    @DisplayName("resetAnalysisResults 메서드")
    class ResetAnalysisResults {

        @Test
        @DisplayName("resetAnalysisResults: verbal/nonverbal 모두 false로 초기화된다")
        void resetAnalysisResults_resetsBothToFalse() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);
            ReflectionTestUtils.setField(analysis, "isVerbalCompleted", true);
            ReflectionTestUtils.setField(analysis, "isNonverbalCompleted", true);

            // when
            analysis.resetAnalysisResults();

            // then
            assertThat(analysis.isVerbalCompleted()).isFalse();
            assertThat(analysis.isNonverbalCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("retry 메서드")
    class Retry {

        @Test
        @DisplayName("retry: FAILED 상태에서 EXTRACTING으로 전이 + 결과/실패사유 리셋")
        void retry_fromFailed() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FAILED);
            ReflectionTestUtils.setField(analysis, "isVerbalCompleted", true);
            ReflectionTestUtils.setField(analysis, "failureReason", "ZOMBIE_TIMEOUT");
            ReflectionTestUtils.setField(analysis, "failureDetail", "detail");

            // when
            analysis.retry();

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.EXTRACTING);
            assertThat(analysis.isVerbalCompleted()).isFalse();
            assertThat(analysis.isNonverbalCompleted()).isFalse();
            assertThat(analysis.getFailureReason()).isNull();
            assertThat(analysis.getFailureDetail()).isNull();
        }

        @Test
        @DisplayName("retry: PARTIAL 상태에서 EXTRACTING으로 전이")
        void retry_fromPartial() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PARTIAL);

            // when
            analysis.retry();

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.EXTRACTING);
        }

        @Test
        @DisplayName("retry: COMPLETED 상태에서 호출 시 예외 발생")
        void retry_fromCompleted_throwsException() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.COMPLETED);

            // when & then
            assertThatThrownBy(analysis::retry)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("markConvertFailed 메서드")
    class MarkConvertFailed {

        @Test
        @DisplayName("markConvertFailed: 상태 전이 + 사유 기록이 원자적으로 실행된다")
        void markConvertFailed_setsStatusAndReason() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);
            ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.PROCESSING);

            // when
            analysis.markConvertFailed("CONVERT_TIMEOUT");

            // then
            assertThat(analysis.getConvertStatus()).isEqualTo(ConvertStatus.FAILED);
            assertThat(analysis.getConvertFailureReason()).isEqualTo("CONVERT_TIMEOUT");
        }
    }

    @Nested
    @DisplayName("updateAnalysisStatus 메서드")
    class UpdateAnalysisStatus {

        @Test
        @DisplayName("updateAnalysisStatus: 유효한 전이(PENDING → PENDING_UPLOAD)가 성공한다")
        void updateAnalysisStatus_validTransition_success() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);

            // when
            analysis.updateAnalysisStatus(AnalysisStatus.PENDING_UPLOAD);

            // then
            assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_UPLOAD);
        }

        @Test
        @DisplayName("updateAnalysisStatus: 유효하지 않은 전이(PENDING → COMPLETED) 시 IllegalStateException이 발생한다")
        void updateAnalysisStatus_invalidTransition_throwsIllegalStateException() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);

            // when & then
            assertThatThrownBy(() -> analysis.updateAnalysisStatus(AnalysisStatus.COMPLETED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("COMPLETED");
        }
    }

    @Nested
    @DisplayName("updateConvertStatus 메서드")
    class UpdateConvertStatus {

        @Test
        @DisplayName("updateConvertStatus: 유효한 전이(PENDING → PROCESSING)가 성공한다")
        void updateConvertStatus_validTransition_success() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);

            // when
            analysis.updateConvertStatus(ConvertStatus.PROCESSING);

            // then
            assertThat(analysis.getConvertStatus()).isEqualTo(ConvertStatus.PROCESSING);
        }

        @Test
        @DisplayName("updateConvertStatus: 유효하지 않은 전이(COMPLETED → PROCESSING) 시 IllegalStateException이 발생한다")
        void updateConvertStatus_invalidTransition_throwsIllegalStateException() {
            // given
            QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);
            ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> analysis.updateConvertStatus(ConvertStatus.PROCESSING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED")
                    .hasMessageContaining("PROCESSING");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private QuestionSetAnalysis createAnalysisInStatus(AnalysisStatus targetStatus) {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .durationMinutes(30)
                .build();

        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(1)
                .build();

        QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                .questionSet(qs)
                .build();

        if (targetStatus != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(analysis, "analysisStatus", targetStatus);
        }
        return analysis;
    }
}
