package com.rehearse.api.domain.feedback.session.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SessionFeedbackPayload(
        OverallSection overall,
        List<StrengthItem> strengths,
        List<GapItem> gaps,
        DeliverySection delivery,
        @JsonProperty("week_plan") List<WeekPlanItem> weekPlan
) {

    public record OverallSection(
            @JsonProperty("dimension_scores") Map<String, Double> dimensionScores,
            @JsonProperty("level_assessment") String levelAssessment,
            String narrative,
            String coverage
    ) {}

    public record StrengthItem(
            String dimension,
            String observation,
            @JsonProperty("why_matters") String whyMatters
    ) {}

    public record GapItem(
            String dimension,
            String observation,
            @JsonProperty("level_gap") String levelGap,
            @JsonProperty("concrete_action") String concreteAction
    ) {}

    public record DeliverySection(
            @JsonProperty("filler_words") String fillerWords,
            @JsonProperty("tone_pattern") String tonePattern,
            String action
    ) {}

    public record WeekPlanItem(
            int priority,
            String topic,
            List<String> resources,
            String practice
    ) {}
}
