package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent;
import com.rehearse.api.domain.interview.event.InterviewCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;

// PR1 중립화: 리스너가 세션 피드백 생성을 호출하지 않고 예외 없이 이벤트를 흡수한다.
// PR2(코멘트 기반 재설계) 시 생성 재활성 + 실패 처리 테스트 재작성 예정.
@DisplayName("SessionFeedbackEventListener - PR1 중립화")
class SessionFeedbackEventListenerTest {

    private final SessionFeedbackEventListener listener = new SessionFeedbackEventListener();

    @Test
    @DisplayName("InterviewCompletedEvent 수신 시 예외 없이 skip 한다")
    void on_interviewCompleted_doesNothing() {
        assertThatCode(() -> listener.on(new InterviewCompletedEvent(1L, LocalDateTime.now())))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DeliveryEnrichmentRequestedEvent 수신 시 예외 없이 skip 한다")
    void on_deliveryEnrichmentRequested_doesNothing() {
        assertThatCode(() -> listener.on(new DeliveryEnrichmentRequestedEvent(2L)))
                .doesNotThrowAnyException();
    }
}
