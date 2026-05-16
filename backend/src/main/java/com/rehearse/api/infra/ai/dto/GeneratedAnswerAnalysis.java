package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedAnswerAnalysis(
        @JsonProperty("claims") List<Claim> claims,
        @JsonProperty("dimension_gaps") Map<String, Integer> dimensionGaps,
        @JsonProperty("weakest_dimension") String weakestDimension,
        @JsonProperty("unstated_assumptions") List<String> unstatedAssumptions,
        @JsonProperty("recommended_next_action") RecommendedNextAction recommendedNextAction
) {

    public GeneratedAnswerAnalysis {
        if (recommendedNextAction == null) {
            throw new IllegalArgumentException(
                    "GeneratedAnswerAnalysis.recommendedNextAction 는 null 일 수 없습니다.");
        }
        claims = claims != null ? List.copyOf(claims) : List.of();
        dimensionGaps = dimensionGaps != null ? Map.copyOf(dimensionGaps) : Map.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public AnswerAnalysis toDomain() {
        return new AnswerAnalysis(claims, dimensionGaps, weakestDimension, unstatedAssumptions, recommendedNextAction);
    }
}
