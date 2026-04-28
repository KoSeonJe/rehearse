package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;

import java.util.List;
import java.util.Map;

public record TurnScoreView(
        Long turnId,
        String rubricId,
        List<String> scoredDimensions,
        Map<String, DimensionScore> dimensionScores,
        TurnStatus status
) {

    public enum TurnStatus {
        OK, FAILED
    }
}
