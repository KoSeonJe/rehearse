package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.interview.event.InterviewCompletedEvent;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionFeedbackEventListener - 이벤트 수신 시 세션 피드백 생성/enrichment 호출")
class SessionFeedbackEventListenerTest {

    @Mock
    private SessionFeedbackService sessionFeedbackService;

    @Mock
    private AiCallMetrics aiCallMetrics;

    @InjectMocks
    private SessionFeedbackEventListener listener;

    @Nested
    @DisplayName("InterviewCompletedEvent 수신")
    class OnInterviewCompleted {

        @Test
        @DisplayName("synthesizePreliminary 를 호출한다")
        void on_interviewCompleted_callsSynthesizePreliminary() {
            listener.on(new InterviewCompletedEvent(1L, LocalDateTime.now()));

            then(sessionFeedbackService).should().synthesizePreliminary(1L);
            then(sessionFeedbackService).should(never()).recordSynthesisFailure(1L, "INTERNAL_ERROR");
        }

        @Test
        @DisplayName("파싱 실패 시 PARSE_FAILED 실패 기록 + 메트릭 증가")
        void on_interviewCompleted_recordsParseFailure() {
            willThrow(new SessionFeedbackParseException("bad json"))
                    .given(sessionFeedbackService).synthesizePreliminary(1L);

            listener.on(new InterviewCompletedEvent(1L, LocalDateTime.now()));

            then(sessionFeedbackService).should().recordSynthesisFailure(1L, "PARSE_FAILED");
            then(aiCallMetrics).should().incrementSynthesizerFailure("PARSE_FAILED");
        }

        @Test
        @DisplayName("그 외 예외 시 INTERNAL_ERROR 실패 기록 + 메트릭 증가")
        void on_interviewCompleted_recordsInternalError() {
            willThrow(new RuntimeException("boom"))
                    .given(sessionFeedbackService).synthesizePreliminary(1L);

            listener.on(new InterviewCompletedEvent(1L, LocalDateTime.now()));

            then(sessionFeedbackService).should().recordSynthesisFailure(1L, "INTERNAL_ERROR");
            then(aiCallMetrics).should().incrementSynthesizerFailure("INTERNAL_ERROR");
        }
    }

    @Nested
    @DisplayName("DeliveryEnrichmentRequestedEvent 수신")
    class OnDeliveryEnrichmentRequested {

        @Test
        @DisplayName("enrichDeliveryFromScores 를 호출한다")
        void on_deliveryEnrichment_callsEnrichDeliveryFromScores() {
            listener.on(new DeliveryEnrichmentRequestedEvent(2L));

            then(sessionFeedbackService).should().enrichDeliveryFromScores(2L);
        }

        @Test
        @DisplayName("파싱 실패 시 PARSE_FAILED 실패 기록 + 메트릭 증가")
        void on_deliveryEnrichment_recordsParseFailure() {
            willThrow(new SessionFeedbackParseException("bad json"))
                    .given(sessionFeedbackService).enrichDeliveryFromScores(2L);

            listener.on(new DeliveryEnrichmentRequestedEvent(2L));

            then(sessionFeedbackService).should().recordSynthesisFailure(2L, "PARSE_FAILED");
            then(aiCallMetrics).should().incrementSynthesizerFailure("PARSE_FAILED");
        }
    }
}
