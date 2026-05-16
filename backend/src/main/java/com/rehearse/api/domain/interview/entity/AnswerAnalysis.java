package com.rehearse.api.domain.interview.entity;

import java.util.List;
import java.util.Map;

public record AnswerAnalysis(
        List<Claim> claims,
        Map<String, Integer> dimensionGaps,
        String weakestDimension,
        List<String> unstatedAssumptions,
        RecommendedNextAction recommendedNextAction
) {

    public AnswerAnalysis {
        claims = claims != null ? List.copyOf(claims) : List.of();
        dimensionGaps = dimensionGaps != null ? Map.copyOf(dimensionGaps) : Map.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public static AnswerAnalysis empty() {
        return new AnswerAnalysis(List.of(), Map.of(), null, List.of(), RecommendedNextAction.CLARIFICATION);
    }
}
