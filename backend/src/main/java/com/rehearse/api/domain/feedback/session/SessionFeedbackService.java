package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler;
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
    private final SessionFeedbackInputAssembler inputAssembler;
    private final SessionFeedbackSynthesizer synthesizer;
    private final LambdaRetryTrigger lambdaRetryTrigger;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final AiCallMetrics aiCallMetrics;

    // F4: LLM 호출이 없는 조회 전용 — readOnly 트랜잭션
    @Transactional(readOnly = true)
    public SessionFeedbackInput assembleInput(Long interviewId) {
        return inputAssembler.assemble(interviewId);
    }

    // F4: LLM 호출 — 트랜잭션 없음 (외부 I/O)
    public SessionFeedbackPayload synthesizePayload(SessionFeedbackInput input) {
        return synthesizer.synthesize(input);
    }

    // F4: DB 쓰기만 담당
    @Transactional
    public void persistPreliminary(Long interviewId, SessionFeedbackPayload payload, String coverage) {
        // 멱등: 이미 존재하면 skip
        if (sessionFeedbackRepository.findByInterviewId(interviewId).isPresent()) {
            log.debug("SessionFeedback 이미 존재 — skip (idempotent): interviewId={}", interviewId);
            return;
        }
        SessionFeedback feedback = SessionFeedback.builder()
                .interviewId(interviewId)
                .overallJson(serialize(payload.overall()))
                .strengthsJson(serialize(payload.strengths()))
                .gapsJson(serialize(payload.gaps()))
                .deliveryJson(null)
                .weekPlanJson(serialize(payload.weekPlan()))
                .coverage(coverage)
                .build();
        sessionFeedbackRepository.save(feedback);
        log.info("SessionFeedback PRELIMINARY 생성 완료: interviewId={}, coverage={}", interviewId, coverage);
    }

    // F4: 진입점 — 트랜잭션 없음 (LLM 포함)
    public void synthesizePreliminary(Long interviewId) {
        if (sessionFeedbackRepository.findByInterviewId(interviewId).isPresent()) {
            log.debug("SessionFeedback 이미 존재 — skip (idempotent): interviewId={}", interviewId);
            return;
        }
        SessionFeedbackInput input = assembleInput(interviewId);
        SessionFeedbackPayload payload = synthesizePayload(input);
        persistPreliminary(interviewId, payload, input.coverage());
    }

    // F4: delivery 조회 전용
    @Transactional(readOnly = true)
    public SessionFeedbackInput assembleInputWithDelivery(Long interviewId, String deliveryJson,
                                                          String visionJson, String nonverbalAggregateJson) {
        return inputAssembler.assembleWithDelivery(interviewId, deliveryJson, visionJson, nonverbalAggregateJson);
    }

    // F4: delivery 쓰기 전용
    @Transactional
    public void persistEnriched(Long interviewId, SessionFeedbackPayload payload, String coverage) {
        SessionFeedback feedback = findByInterviewIdOrThrow(interviewId);
        // F13: applyDeliveryEnrichment 사용
        feedback.applyDeliveryEnrichment(serialize(payload.delivery()));
        feedback.updateCoverage(coverage);
        log.info("SessionFeedback COMPLETE 전환 완료: interviewId={}", interviewId);
    }

    // F4: 진입점 — 트랜잭션 없음 (LLM 포함)
    public void enrichDelivery(Long interviewId, String deliveryJson, String visionJson, String nonverbalAggregateJson) {
        if (deliveryJson == null && visionJson == null && nonverbalAggregateJson == null) {
            // F2: null delivery → markCompleteWithFailure("TIMEOUT")
            markCompleteDueToTimeout(interviewId);
            log.info("Delivery null — PRELIMINARY→COMPLETE(TIMEOUT) 전환: interviewId={}", interviewId);
            return;
        }

        SessionFeedbackInput input = assembleInputWithDelivery(interviewId, deliveryJson, visionJson, nonverbalAggregateJson);
        SessionFeedbackPayload payload = synthesizePayload(input);
        persistEnriched(interviewId, payload, input.coverage());
    }

    @Transactional
    public void markCompleteDueToTimeout(Long interviewId) {
        SessionFeedback feedback = findByInterviewIdOrThrow(interviewId);
        // F2: TIMEOUT은 admin retry 가능 상태로 보존
        feedback.markCompleteWithFailure("TIMEOUT");
        log.info("SessionFeedback watchdog timeout → COMPLETE(retryable): interviewId={}", interviewId);
    }

    // F1: synthesize 실패 시 placeholder row 보존
    @Transactional
    public void recordSynthesisFailure(Long interviewId, String reason) {
        boolean retryable = SessionFeedbackFailurePolicy.isRetryable(reason);
        sessionFeedbackRepository.findByInterviewId(interviewId).ifPresentOrElse(
                feedback -> {
                    feedback.recordFailure(reason, retryable);
                    log.warn("SessionFeedback 기존 row failure 갱신: interviewId={}, reason={}", interviewId, reason);
                },
                () -> {
                    // 행이 없으면 빈 PRELIMINARY 행 insert
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

        // F2: COMPLETE 이면서 retryable=false 이거나 lastFailureReason=null 이면 차단
        if (feedback.getStatus() == SessionFeedbackStatus.COMPLETE
                && (!feedback.isDeliveryRetryable() || feedback.getLastFailureReason() == null)) {
            throw new SessionFeedbackBusyException();
        }

        // F3: 60초 cooldown 가드
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
            // F3: 동시 retry 충돌 → 409
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

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
