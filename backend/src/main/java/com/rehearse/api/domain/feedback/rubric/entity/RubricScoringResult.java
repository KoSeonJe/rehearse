package com.rehearse.api.domain.feedback.rubric.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RubricScoringResult(
        String rubricId,
        List<String> scoredDimensions,
        Map<String, DimensionScore> dimensionScores,
        String levelFlag
) {

    public static RubricScoringResult empty(String rubricId) {
        return new RubricScoringResult(rubricId, List.of(), Map.of(), null);
    }

    public static RubricScoringResult notEvaluable(String rubricId,
                                                    List<String> dimensions,
                                                    String reason) {
        Map<String, DimensionScore> map = new LinkedHashMap<>();
        for (String dim : dimensions) {
            map.put(dim, DimensionScore.notEvaluable(reason));
        }
        return new RubricScoringResult(rubricId, List.copyOf(dimensions), Map.copyOf(map), null);
    }

    public boolean isEmpty() {
        return scoredDimensions == null || scoredDimensions.isEmpty();
    }
}
