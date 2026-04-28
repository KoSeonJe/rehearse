package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackBusyException;
import com.rehearse.api.domain.feedback.session.infra.LambdaRetryTrigger;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackSynthesizer;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackServiceTest {

    @Mock private SessionFeedbackRepository sessionFeedbackRepository;
    @Mock private SessionFeedbackPersistenceService persistenceService;
    @Mock private SessionFeedbackSynthesizer synthesizer;
    @Mock private LambdaRetryTrigger lambdaRetryTrigger;
    @Mock private AiCallMetrics aiCallMetrics;

    private SessionFeedbackService service;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new SessionFeedbackService(
                sessionFeedbackRepository, persistenceService, synthesizer,
                lambdaRetryTrigger, new ObjectMapper(), meterRegistry, aiCallMetrics);
    }

    @Test
    @DisplayName("synthesizePreliminary_이미_row가_존재하면_synthesizer를_호출하지_않는다")
    void synthesizePreliminary_is_idempotent_when_row_exists() {
        given(sessionFeedbackRepository.findByInterviewId(1L))
                .willReturn(Optional.of(buildFeedback(SessionFeedbackStatus.PRELIMINARY)));

        service.synthesizePreliminary(1L);

        then(synthesizer).should(never()).synthesize(any());
    }

    @Test
    @DisplayName("recordSynthesisFailure_row가_없으면_PRELIMINARY_placeholder_insert한다")
    void recordSynthesisFailure_inserts_placeholder_when_no_row() {
        given(sessionFeedbackRepository.findByInterviewId(2L)).willReturn(Optional.empty());

        service.recordSynthesisFailure(2L, "PARSE_FAILED");

        then(sessionFeedbackRepository).should(times(1)).save(any(SessionFeedback.class));
    }

    @Test
    @DisplayName("recordSynthesisFailure_row가_이미_존재하면_failure만_갱신한다")
    void recordSynthesisFailure_updates_existing_row() {
        SessionFeedback existing = buildFeedback(SessionFeedbackStatus.PRELIMINARY);
        given(sessionFeedbackRepository.findByInterviewId(3L)).willReturn(Optional.of(existing));

        service.recordSynthesisFailure(3L, "INTERNAL_ERROR");

        assertThat(existing.getLastFailureReason()).isEqualTo("INTERNAL_ERROR");
        then(sessionFeedbackRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("markCompleteDueToTimeout_TIMEOUT으로_markCompleteWithFailure를_호출한다")
    void markCompleteDueToTimeout_calls_markCompleteWithFailure_with_TIMEOUT() {
        SessionFeedback feedback = buildFeedback(SessionFeedbackStatus.PRELIMINARY);
        given(sessionFeedbackRepository.findByInterviewId(4L)).willReturn(Optional.of(feedback));

        service.markCompleteDueToTimeout(4L);

        assertThat(feedback.getStatus()).isEqualTo(SessionFeedbackStatus.COMPLETE);
        assertThat(feedback.getLastFailureReason()).isEqualTo("TIMEOUT");
        assertThat(feedback.isDeliveryRetryable()).isTrue();
    }

    @Test
    @DisplayName("retryDelivery_COMPLETE_상태에서_retryable=true_lastFailureReason_있으면_허용")
    void retryDelivery_allows_complete_with_retryable_and_reason() {
        SessionFeedback feedback = buildFeedbackWithFailure("TIMEOUT");
        given(sessionFeedbackRepository.findByInterviewId(5L)).willReturn(Optional.of(feedback));

        service.retryDelivery(5L, 99L);

        then(lambdaRetryTrigger).should(times(1)).trigger(5L);
    }

    @Test
    @DisplayName("retryDelivery_COMPLETE_상태에서_retryable=false면_BusyException_발생")
    void retryDelivery_throws_when_complete_and_not_retryable() {
        SessionFeedback feedback = buildFeedbackNotRetryable();
        given(sessionFeedbackRepository.findByInterviewId(6L)).willReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.retryDelivery(6L, 99L))
                .isInstanceOf(SessionFeedbackBusyException.class);
    }

    @Test
    @DisplayName("retryDelivery_60초_cooldown_중이면_BusyException_발생")
    void retryDelivery_throws_when_cooling_down() {
        SessionFeedback feedback = buildFeedbackWithFailure("TIMEOUT");
        // 이미 한 번 retry → retryStartedAt = now
        feedback.incrementRetry();
        given(sessionFeedbackRepository.findByInterviewId(7L)).willReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.retryDelivery(7L, 99L))
                .isInstanceOf(SessionFeedbackBusyException.class);
    }

    @Test
    @DisplayName("recordFailure_6종_reason_모두_retryable=true로_매핑된다")
    void recordFailure_all_six_reasons_are_retryable() {
        String[] reasons = {"TIMEOUT", "VISION_ERROR", "API_ERROR",
                "TRANSCRIPTION_ERROR", "INTERNAL_ERROR", "SCHEMA_MISSING_FIELDS"};

        for (String reason : reasons) {
            SessionFeedback feedback = buildFeedback(SessionFeedbackStatus.PRELIMINARY);
            given(sessionFeedbackRepository.findByInterviewId(any())).willReturn(Optional.of(feedback));
            service.recordFailure(99L, reason);
            assertThat(feedback.isDeliveryRetryable())
                    .as("reason=%s should be retryable", reason)
                    .isTrue();
        }
    }

    // --- helpers ---

    private SessionFeedback buildFeedback(SessionFeedbackStatus status) {
        SessionFeedback f = SessionFeedback.builder()
                .interviewId(1L)
                .overallJson(null).strengthsJson(null).gapsJson(null)
                .deliveryJson(null).weekPlanJson(null).coverage(null)
                .build();
        if (status == SessionFeedbackStatus.COMPLETE) {
            f.markComplete();
        }
        return f;
    }

    private SessionFeedback buildFeedbackWithFailure(String reason) {
        SessionFeedback f = buildFeedback(SessionFeedbackStatus.PRELIMINARY);
        f.markCompleteWithFailure(reason);
        return f;
    }

    private SessionFeedback buildFeedbackNotRetryable() {
        SessionFeedback f = buildFeedback(SessionFeedbackStatus.PRELIMINARY);
        f.markComplete();
        f.recordFailure(null, false);
        return f;
    }
}
