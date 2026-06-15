package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackWatchdogTest {

    @Mock private SessionFeedbackRepository sessionFeedbackRepository;
    @Mock private SessionFeedbackService sessionFeedbackService;

    @InjectMocks
    private SessionFeedbackWatchdog watchdog;

    @Test
    @DisplayName("cutoff_이전_PRELIMINARY_row는_markCompleteDueToTimeout_호출")
    void checkStalePreliminaries_calls_markComplete_for_stale_rows() {
        ReflectionTestUtils.setField(watchdog, "deliveryTimeoutMinutes", 10);
        SessionFeedback stale = buildFeedback(1L);
        given(sessionFeedbackRepository.findByStatusAndCreatedAtBefore(
                eq(SessionFeedbackStatus.PRELIMINARY), any(LocalDateTime.class)))
                .willReturn(List.of(stale));
        willDoNothing().given(sessionFeedbackService).markCompleteDueToTimeout(1L);

        watchdog.checkStalePreliminaries();

        then(sessionFeedbackService).should(times(1)).markCompleteDueToTimeout(1L);
    }

    @Test
    @DisplayName("stale_row_없으면_markCompleteDueToTimeout_미호출")
    void checkStalePreliminaries_does_nothing_when_no_stale_rows() {
        ReflectionTestUtils.setField(watchdog, "deliveryTimeoutMinutes", 10);
        given(sessionFeedbackRepository.findByStatusAndCreatedAtBefore(
                eq(SessionFeedbackStatus.PRELIMINARY), any(LocalDateTime.class)))
                .willReturn(List.of());

        watchdog.checkStalePreliminaries();

        then(sessionFeedbackService).should(never()).markCompleteDueToTimeout(any());
    }

    @Test
    @DisplayName("markCompleteDueToTimeout_예외_발생해도_다음_row_처리_계속")
    void checkStalePreliminaries_continues_on_exception() {
        ReflectionTestUtils.setField(watchdog, "deliveryTimeoutMinutes", 10);
        SessionFeedback stale1 = buildFeedback(1L);
        SessionFeedback stale2 = buildFeedback(2L);
        given(sessionFeedbackRepository.findByStatusAndCreatedAtBefore(
                eq(SessionFeedbackStatus.PRELIMINARY), any(LocalDateTime.class)))
                .willReturn(List.of(stale1, stale2));
        willThrow(new RuntimeException("DB 오류"))
                .given(sessionFeedbackService).markCompleteDueToTimeout(1L);
        willDoNothing().given(sessionFeedbackService).markCompleteDueToTimeout(2L);

        watchdog.checkStalePreliminaries();

        then(sessionFeedbackService).should(times(1)).markCompleteDueToTimeout(2L);
    }

    private SessionFeedback buildFeedback(Long interviewId) {
        return SessionFeedback.builder()
                .interviewId(interviewId)
                .overallJson(null).strengthsJson(null).gapsJson(null)
                .deliveryJson(null).weekPlanJson(null).coverage(null)
                .build();
    }
}
