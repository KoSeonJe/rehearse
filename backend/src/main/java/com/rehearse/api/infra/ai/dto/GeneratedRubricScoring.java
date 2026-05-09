package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;

import java.util.List;
import java.util.Map;

public record GeneratedRubricScoring(
        String rubricId,
        List<String> scoredDimensions,
        Map<String, DimensionScore> dimensionScores,
        String levelFlag
) {

    private static final int SCORE_MIN = 1;
    private static final int SCORE_MAX = 3;

    public GeneratedRubricScoring {
        scoredDimensions = scoredDimensions != null ? List.copyOf(scoredDimensions) : List.of();
        dimensionScores = dimensionScores != null ? Map.copyOf(dimensionScores) : Map.of();
        for (Map.Entry<String, DimensionScore> entry : dimensionScores.entrySet()) {
            DimensionScore ds = entry.getValue();
            if (ds == null || ds.score() == null) {
                continue;
            }
            int score = ds.score();
            if (score < SCORE_MIN || score > SCORE_MAX) {
                throw new IllegalArgumentException(
                        "GeneratedRubricScoring.dimensionScores[" + entry.getKey()
                                + "].score 는 1~3 범위여야 합니다: " + score);
            }
        }
    }

    public RubricScoringResult toDomain() {
        return new RubricScoringResult(rubricId, scoredDimensions, dimensionScores, levelFlag);
    }
}
