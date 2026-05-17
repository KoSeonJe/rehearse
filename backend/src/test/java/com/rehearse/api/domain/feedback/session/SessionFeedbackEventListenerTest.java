package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.rubric.service.RubricBackfillScorer;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.interview.event.InterviewCompletedEvent;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackEventListenerTest {

    @Mock private SessionFeedbackService sessionFeedbackService;
    @Mock private RubricBackfillScorer rubricBackfillScorer;
    @Mock private AiCallMetrics aiCallMetrics;

    @InjectMocks
    private SessionFeedbackEventListener listener;

    @Test
    @DisplayName("정상_케이스_backfill_후_synthesizePreliminary_1회_호출")
    void on_calls_backfill_then_synthesizePreliminary_once_on_success() {
        willDoNothing().given(sessionFeedbackService).synthesizePreliminary(1L);

        listener.on(new InterviewCompletedEvent(1L, java.time.LocalDateTime.now()));

        then(rubricBackfillScorer).should(times(1)).backfill(1L);
        then(sessionFeedbackService).should(times(1)).synthesizePreliminary(1L);
        then(sessionFeedbackService).should(times(0)).recordSynthesisFailure(1L, "PARSE_FAILED");
    }

    @Test
    @DisplayName("backfill_실패해도_synthesizePreliminary_호출_되고_synth_실패_카운터는_증가하지_않는다")
    void on_continues_to_synthesis_when_backfill_throws() {
        willThrow(new RuntimeException("backfill 폭발"))
                .given(rubricBackfillScorer).backfill(5L);
        willDoNothing().given(sessionFeedbackService).synthesizePreliminary(5L);

        listener.on(new InterviewCompletedEvent(5L, java.time.LocalDateTime.now()));

        then(sessionFeedbackService).should(times(1)).synthesizePreliminary(5L);
        then(aiCallMetrics).should(times(1)).incrementRubricFailure("backfill_listener_failed");
        then(aiCallMetrics).should(times(0)).incrementSynthesizerFailure("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("parse_실패_시_recordSynthesisFailure_PARSE_FAILED_호출")
    void on_records_parse_failure_when_parse_exception_thrown() {
        willThrow(new SessionFeedbackParseException("파싱 실패"))
                .given(sessionFeedbackService).synthesizePreliminary(2L);

        listener.on(new InterviewCompletedEvent(2L, java.time.LocalDateTime.now()));

        then(sessionFeedbackService).should(times(1)).recordSynthesisFailure(2L, "PARSE_FAILED");
        then(aiCallMetrics).should(times(1)).incrementSynthesizerFailure("PARSE_FAILED");
    }

    @Test
    @DisplayName("일반_예외_시_INTERNAL_ERROR_카운터_증가")
    void on_records_internal_error_when_generic_exception_thrown() {
        willThrow(new RuntimeException("예상치 못한 오류"))
                .given(sessionFeedbackService).synthesizePreliminary(3L);

        listener.on(new InterviewCompletedEvent(3L, java.time.LocalDateTime.now()));

        then(sessionFeedbackService).should(times(1)).recordSynthesisFailure(3L, "INTERNAL_ERROR");
        then(aiCallMetrics).should(times(1)).incrementSynthesizerFailure("INTERNAL_ERROR");
    }
}
