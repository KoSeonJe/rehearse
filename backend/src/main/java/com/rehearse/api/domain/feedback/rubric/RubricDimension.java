package com.rehearse.api.domain.feedback.rubric;

import java.util.Map;

public record RubricDimension(
        String id,
        String name,
        String description,
        String scope,
        Map<Integer, ScoringLevel> scoring
) {

    public record ScoringLevel(String label, java.util.List<String> observable) {}
}
