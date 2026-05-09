package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.AnswerFeedbackPerspective;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedAnswerAnalysis(
        @JsonProperty("turn_id") long turnId,
        @JsonProperty("claims") List<Claim> claims,
        @JsonProperty("missing_perspectives") List<AnswerFeedbackPerspective> missingPerspectives,
        @JsonProperty("unstated_assumptions") List<String> unstatedAssumptions,
        @JsonProperty("answer_quality") int answerQuality,
        @JsonProperty("recommended_next_action") RecommendedNextAction recommendedNextAction
) {

    public GeneratedAnswerAnalysis {
        if (answerQuality < 1 || answerQuality > 5) {
            throw new IllegalArgumentException(
                    "GeneratedAnswerAnalysis.answerQuality 는 1~5 범위여야 합니다: " + answerQuality);
        }
        if (recommendedNextAction == null) {
            throw new IllegalArgumentException(
                    "GeneratedAnswerAnalysis.recommendedNextAction 는 null 일 수 없습니다.");
        }
        claims = claims != null ? List.copyOf(claims) : List.of();
        missingPerspectives = missingPerspectives != null ? List.copyOf(missingPerspectives) : List.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public AnswerAnalysis toDomain() {
        return new AnswerAnalysis(turnId, claims, missingPerspectives, unstatedAssumptions,
                answerQuality, recommendedNextAction);
    }
}
