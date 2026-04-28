package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackPayload;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackErrorCode;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionFeedbackPersistenceService {

    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final SessionFeedbackInputAssembler inputAssembler;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public SessionFeedbackInput assembleInput(Long interviewId) {
        return inputAssembler.assemble(interviewId);
    }

    @Transactional(readOnly = true)
    public SessionFeedbackInput assembleInputWithDelivery(Long interviewId, String deliveryJson,
                                                          String visionJson, String nonverbalAggregateJson) {
        return inputAssembler.assembleWithDelivery(interviewId, deliveryJson, visionJson, nonverbalAggregateJson);
    }

    @Transactional
    public void persistPreliminary(Long interviewId, SessionFeedbackPayload payload, String coverage) {
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

    @Transactional
    public void persistEnriched(Long interviewId, SessionFeedbackPayload payload, String coverage) {
        SessionFeedback feedback = sessionFeedbackRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(SessionFeedbackErrorCode.NOT_FOUND));
        feedback.applyDeliveryEnrichment(serialize(payload.delivery()));
        feedback.updateCoverage(coverage);
        log.info("SessionFeedback COMPLETE 전환 완료: interviewId={}", interviewId);
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
