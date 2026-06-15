package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedAnswerAnalysis(
        @JsonProperty("transcript") String transcript,
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
        // audio chat (JSON_OBJECT) 경로는 schema 미적용 → 방어적 null 필터링 유지.
        dimensionGaps = dimensionGaps != null ? copyNonNull(dimensionGaps) : Map.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public AnswerAnalysis toDomain() {
        return new AnswerAnalysis(
                transcript != null ? transcript : "",
                claims, dimensionGaps, weakestDimension, unstatedAssumptions, recommendedNextAction);
    }

    private static Map<String, Integer> copyNonNull(Map<String, Integer> source) {
        Map<String, Integer> filtered = new HashMap<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getValue() != null) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(filtered);
    }
}
