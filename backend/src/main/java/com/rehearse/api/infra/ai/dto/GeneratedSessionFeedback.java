package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.feedback.session.vo.DeliverySection;
import com.rehearse.api.domain.feedback.session.vo.GapItem;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import com.rehearse.api.domain.feedback.session.vo.StrengthItem;
import com.rehearse.api.domain.feedback.session.vo.WeekPlanItem;

import java.util.List;

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
        strengths = strengths != null ? List.copyOf(strengths) : List.of();
        gaps = gaps != null ? List.copyOf(gaps) : List.of();
        weekPlan = weekPlan != null ? List.copyOf(weekPlan) : List.of();
    }
}
