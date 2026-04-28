package com.rehearse.api.domain.feedback.rubric.entity;

import java.util.List;
import java.util.Map;

public record RubricScore(
        String rubricId,
        List<String> scoredDimensions,
        Map<String, DimensionScore> dimensionScores,
        String levelFlag
) {

    public static RubricScore empty(String rubricId) {
        return new RubricScore(rubricId, List.of(), Map.of(), null);
    }

    public boolean isEmpty() {
        return scoredDimensions == null || scoredDimensions.isEmpty();
    }
}
