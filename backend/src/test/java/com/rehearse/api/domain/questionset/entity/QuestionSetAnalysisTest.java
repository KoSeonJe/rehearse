package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionSetAnalysisTest {

    // ─────────────────────────────────────────────────────────────
    // completeAnalysis
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("completeAnalysis(true, true) → COMPLETED")
    void completeAnalysis_bothTrue_returnsCompleted() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

        analysis.completeAnalysis(true, true);

        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(analysis.isVerbalCompleted()).isTrue();
        assertThat(analysis.isNonverbalCompleted()).isTrue();
    }

    @Test
    @DisplayName("completeAnalysis(true, false) → PARTIAL")
    void completeAnalysis_verbalOnly_returnsPartial() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

        analysis.completeAnalysis(true, false);

        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
        assertThat(analysis.isVerbalCompleted()).isTrue();
        assertThat(analysis.isNonverbalCompleted()).isFalse();
    }

    @Test
    @DisplayName("completeAnalysis(false, true) → PARTIAL")
    void completeAnalysis_nonverbalOnly_returnsPartial() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

        analysis.completeAnalysis(false, true);

        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
        assertThat(analysis.isVerbalCompleted()).isFalse();
        assertThat(analysis.isNonverbalCompleted()).isTrue();
    }

    @Test
    @DisplayName("completeAnalysis(false, false) → FAILED")
    void completeAnalysis_bothFalse_returnsFailed() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.FINALIZING);

        analysis.completeAnalysis(false, false);

        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.isVerbalCompleted()).isFalse();
        assertThat(analysis.isNonverbalCompleted()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // isFullyReady
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isFullyReady: analysisStatus=COMPLETED, convertStatus=COMPLETED → true")
    void isFullyReady_completedAndConverted_returnsTrue() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.COMPLETED);
        ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.COMPLETED);

        assertThat(analysis.isFullyReady()).isTrue();
    }

    @Test
    @DisplayName("isFullyReady: analysisStatus=PARTIAL, convertStatus=COMPLETED → true")
    void isFullyReady_partialAndConverted_returnsTrue() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PARTIAL);
        ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.COMPLETED);

        assertThat(analysis.isFullyReady()).isTrue();
    }

    @Test
    @DisplayName("isFullyReady: analysisStatus=COMPLETED, convertStatus=PENDING → false")
    void isFullyReady_completedButConvertPending_returnsFalse() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.COMPLETED);
        // convertStatus 기본값은 PENDING

        assertThat(analysis.isFullyReady()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // markFailed
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("markFailed: failureReason과 failureDetail이 설정되고 FAILED 상태로 전이된다")
    void markFailed_setsReasonAndDetail() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.ANALYZING);

        analysis.markFailed("ZOMBIE_TIMEOUT", "분석이 10분 내 완료되지 않음");

        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getFailureReason()).isEqualTo("ZOMBIE_TIMEOUT");
        assertThat(analysis.getFailureDetail()).isEqualTo("분석이 10분 내 완료되지 않음");
    }

    // ─────────────────────────────────────────────────────────────
    // resetVerbalResult / resetNonverbalResult
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resetVerbalResult: isVerbalCompleted가 false로 초기화된다")
    void resetVerbalResult_setsVerbalCompletedToFalse() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);
        ReflectionTestUtils.setField(analysis, "isVerbalCompleted", true);

        analysis.resetVerbalResult();

        assertThat(analysis.isVerbalCompleted()).isFalse();
    }

    @Test
    @DisplayName("resetNonverbalResult: isNonverbalCompleted가 false로 초기화된다")
    void resetNonverbalResult_setsNonverbalCompletedToFalse() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);
        ReflectionTestUtils.setField(analysis, "isNonverbalCompleted", true);

        analysis.resetNonverbalResult();

        assertThat(analysis.isNonverbalCompleted()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // updateAnalysisStatus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAnalysisStatus: 유효한 전이(PENDING → PENDING_UPLOAD)가 성공한다")
    void updateAnalysisStatus_validTransition_success() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);

        analysis.updateAnalysisStatus(AnalysisStatus.PENDING_UPLOAD);

        assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING_UPLOAD);
    }

    @Test
    @DisplayName("updateAnalysisStatus: 유효하지 않은 전이(PENDING → COMPLETED) 시 IllegalStateException이 발생한다")
    void updateAnalysisStatus_invalidTransition_throwsIllegalStateException() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);

        assertThatThrownBy(() -> analysis.updateAnalysisStatus(AnalysisStatus.COMPLETED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING")
                .hasMessageContaining("COMPLETED");
    }

    // ─────────────────────────────────────────────────────────────
    // updateConvertStatus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateConvertStatus: 유효한 전이(PENDING → PROCESSING)가 성공한다")
    void updateConvertStatus_validTransition_success() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);

        analysis.updateConvertStatus(ConvertStatus.PROCESSING);

        assertThat(analysis.getConvertStatus()).isEqualTo(ConvertStatus.PROCESSING);
    }

    @Test
    @DisplayName("updateConvertStatus: 유효하지 않은 전이(COMPLETED → PROCESSING) 시 IllegalStateException이 발생한다")
    void updateConvertStatus_invalidTransition_throwsIllegalStateException() {
        QuestionSetAnalysis analysis = createAnalysisInStatus(AnalysisStatus.PENDING);
        ReflectionTestUtils.setField(analysis, "convertStatus", ConvertStatus.COMPLETED);

        assertThatThrownBy(() -> analysis.updateConvertStatus(ConvertStatus.PROCESSING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("PROCESSING");
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
                .category(QuestionCategory.CS)
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
