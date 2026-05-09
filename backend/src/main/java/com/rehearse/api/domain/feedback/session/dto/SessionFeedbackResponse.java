package com.rehearse.api.domain.feedback.session.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SessionFeedbackResponse {

    private final Long id;
    private final Long interviewId;
    private final SessionFeedbackStatus status;
    private final OverallSection overall;
    private final List<StrengthItem> strengths;
    private final List<GapItem> gaps;
    private final DeliverySection delivery;
    private final List<WeekPlanItem> weekPlan;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverallSection(
            @JsonProperty("dimension_scores") Map<String, Double> dimensionScores,
            @JsonProperty("level_assessment") String levelAssessment,
            String narrative,
            String coverage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StrengthItem(
            String dimension,
            String observation,
            @JsonProperty("why_matters") String whyMatters
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GapItem(
            String dimension,
            String observation,
            @JsonProperty("level_gap") String levelGap,
            @JsonProperty("concrete_action") String concreteAction
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeliverySection(
            @JsonProperty("filler_words") String fillerWords,
            @JsonProperty("tone_pattern") String tonePattern,
            String action
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeekPlanItem(
            int priority,
            String topic,
            List<String> resources,
            String practice
    ) {}
}
