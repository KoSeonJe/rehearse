package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackPayload;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackResponse;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackBusyException;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackErrorCode;
import com.rehearse.api.domain.feedback.session.infra.LambdaRetryTrigger;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackSynthesizer;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionFeedbackService {

    private static final String LAMBDA_RETRY_SUCCESS = "rehearse.ai.lambda.retry.success";
    private static final String LAMBDA_RETRY_FAILURE = "rehearse.ai.lambda.retry.failure";

    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final SessionFeedbackPersistenceService persistenceService;
    private final SessionFeedbackSynthesizer synthesizer;
    private final LambdaRetryTrigger lambdaRetryTrigger;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final AiCallMetrics aiCallMetrics;

    public void synthesizePreliminary(Long interviewId) {
        if (sessionFeedbackRepository.findByInterviewId(interviewId).isPresent()) {
            log.debug("SessionFeedback 이미 존재 — skip (idempotent): interviewId={}", interviewId);
            return;
        }
        SessionFeedbackInput input = persistenceService.assembleInput(interviewId);
        SessionFeedbackPayload payload = synthesizer.synthesize(input);
        persistenceService.persistPreliminary(interviewId, payload, input.coverage());
    }

    public void enrichDelivery(Long interviewId, String deliveryJson, String visionJson, String nonverbalAggregateJson) {
        if (deliveryJson == null && visionJson == null && nonverbalAggregateJson == null) {
            markCompleteDueToTimeout(interviewId);
            log.info("Delivery null — PRELIMINARY→COMPLETE(TIMEOUT) 전환: interviewId={}", interviewId);
            return;
        }

        SessionFeedbackInput input = persistenceService.assembleInputWithDelivery(
                interviewId, deliveryJson, visionJson, nonverbalAggregateJson);
        SessionFeedbackPayload payload = synthesizer.synthesize(input);
        persistenceService.persistEnriched(interviewId, payload, input.coverage());
    }

    @Transactional
    public void markCompleteDueToTimeout(Long interviewId) {
        SessionFeedback feedback = findByInterviewIdOrThrow(interviewId);
        feedback.markCompleteWithFailure("TIMEOUT");
        log.info("SessionFeedback watchdog timeout → COMPLETE(retryable): interviewId={}", interviewId);
    }

    @Transactional
    public void recordSynthesisFailure(Long interviewId, String reason) {
        boolean retryable = SessionFeedbackFailurePolicy.isRetryable(reason);
        sessionFeedbackRepository.findByInterviewId(interviewId).ifPresentOrElse(
                feedback -> {
                    feedback.recordFailure(reason, retryable);
                    log.warn("SessionFeedback 기존 row failure 갱신: interviewId={}, reason={}", interviewId, reason);
                },
                () -> {
                    SessionFeedback placeholder = SessionFeedback.builder()
                            .interviewId(interviewId)
                            .overallJson(null)
                            .strengthsJson(null)
                            .gapsJson(null)
                            .deliveryJson(null)
                            .weekPlanJson(null)
                            .coverage(null)
                            .build();
                    placeholder.recordFailure(reason, retryable);
                    sessionFeedbackRepository.save(placeholder);
                    log.warn("SessionFeedback placeholder PRELIMINARY 생성: interviewId={}, reason={}", interviewId, reason);
                }
        );
    }

    @Transactional
    public void recordFailure(Long interviewId, String reason) {
        SessionFeedback feedback = findByInterviewIdOrThrow(interviewId);
        boolean retryable = SessionFeedbackFailurePolicy.isRetryable(reason);
        feedback.recordFailure(mapFailureReason(reason), retryable);
        log.warn("SessionFeedback failure 기록: interviewId={}, reason={}", interviewId, reason);
    }

    @Transactional
    public void retryDelivery(Long interviewId, Long adminUserId) {
        SessionFeedback feedback = findByInterviewIdOrThrow(interviewId);

        if (feedback.getStatus() == SessionFeedbackStatus.COMPLETE
                && (!feedback.isDeliveryRetryable() || feedback.getLastFailureReason() == null)) {
            throw new SessionFeedbackBusyException();
        }

        if (feedback.isRetryCoolingDown()) {
            throw new SessionFeedbackBusyException();
        }

        feedback.incrementRetry();

        try {
            lambdaRetryTrigger.trigger(interviewId);
            meterRegistry.counter(LAMBDA_RETRY_SUCCESS, "interviewId", String.valueOf(interviewId)).increment();
            log.info("Lambda retry 트리거: interviewId={}, adminUserId={}, attempts={}",
                    interviewId, adminUserId, feedback.getRetryAttempts());
        } catch (OptimisticLockingFailureException e) {
            throw new SessionFeedbackBusyException();
        } catch (Exception e) {
            meterRegistry.counter(LAMBDA_RETRY_FAILURE, "interviewId", String.valueOf(interviewId)).increment();
            log.error("Lambda retry 트리거 실패: interviewId={}, reason={}", interviewId, e.getMessage());
            throw e;
        }
    }

    public SessionFeedbackResponse getByInterview(Long interviewId) {
        SessionFeedback feedback = findByInterviewIdOrThrow(interviewId);
        return SessionFeedbackResponse.from(feedback, objectMapper);
    }

    private SessionFeedback findByInterviewIdOrThrow(Long interviewId) {
        return sessionFeedbackRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(SessionFeedbackErrorCode.NOT_FOUND));
    }

    private String mapFailureReason(String reason) {
        if (reason == null) return "UNKNOWN";
        return switch (reason.toUpperCase()) {
            case "TIMEOUT" -> "TIMEOUT";
            case "VISION_ERROR" -> "VISION_ERROR";
            case "API_ERROR" -> "API_ERROR";
            case "TRANSCRIPTION_ERROR" -> "TRANSCRIPTION_ERROR";
            case "INTERNAL_ERROR" -> "INTERNAL_ERROR";
            case "SCHEMA_MISSING_FIELDS" -> "SCHEMA_MISSING_FIELDS";
            default -> reason.length() > 64 ? reason.substring(0, 64) : reason;
        };
    }
}
