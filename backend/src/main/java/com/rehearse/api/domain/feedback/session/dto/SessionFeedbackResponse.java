package com.rehearse.api.domain.feedback.session.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SessionFeedbackResponse {

    private final Long id;
    private final Long interviewId;
    private final SessionFeedbackStatus status;
    private final SessionFeedbackPayload.OverallSection overall;
    private final List<SessionFeedbackPayload.StrengthItem> strengths;
    private final List<SessionFeedbackPayload.GapItem> gaps;
    private final SessionFeedbackPayload.DeliverySection delivery;
    private final List<SessionFeedbackPayload.WeekPlanItem> weekPlan;
    private final String coverage;
    private final boolean deliveryRetryable;
    private final String lastFailureReason;
    private final int retryAttempts;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static SessionFeedbackResponse from(SessionFeedback entity, ObjectMapper objectMapper) {
        return SessionFeedbackResponse.builder()
                .id(entity.getId())
                .interviewId(entity.getInterviewId())
                .status(entity.getStatus())
                .overall(parseJson(entity.getOverallJson(), new TypeReference<>() {}, objectMapper))
                .strengths(parseJson(entity.getStrengthsJson(), new TypeReference<>() {}, objectMapper))
                .gaps(parseJson(entity.getGapsJson(), new TypeReference<>() {}, objectMapper))
                .delivery(parseJson(entity.getDeliveryJson(), new TypeReference<>() {}, objectMapper))
                .weekPlan(parseJson(entity.getWeekPlanJson(), new TypeReference<>() {}, objectMapper))
                .coverage(entity.getCoverage())
                .deliveryRetryable(entity.isDeliveryRetryable())
                .lastFailureReason(entity.getLastFailureReason())
                .retryAttempts(entity.getRetryAttempts())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static <T> T parseJson(String json, TypeReference<T> typeRef, ObjectMapper objectMapper) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
