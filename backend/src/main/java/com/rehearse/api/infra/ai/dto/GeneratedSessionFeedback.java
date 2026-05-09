package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedSessionFeedback(
        OverallSection overall,
        List<StrengthItem> strengths,
        List<GapItem> gaps,
        DeliverySection delivery,
        @JsonProperty("week_plan") List<WeekPlanItem> weekPlan
) {

    public GeneratedSessionFeedback {
        if (overall == null) {
            throw new IllegalArgumentException("GeneratedSessionFeedback.overall 는 null 일 수 없습니다.");
        }
        if (delivery == null) {
            throw new IllegalArgumentException("GeneratedSessionFeedback.delivery 는 null 일 수 없습니다.");
        }
        strengths = strengths != null ? List.copyOf(strengths) : List.of();
        gaps = gaps != null ? List.copyOf(gaps) : List.of();
        weekPlan = weekPlan != null ? List.copyOf(weekPlan) : List.of();
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
