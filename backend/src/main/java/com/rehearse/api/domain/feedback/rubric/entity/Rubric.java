package com.rehearse.api.domain.feedback.rubric.entity;

import com.rehearse.api.domain.interview.entity.InterviewLevel;

import java.util.List;
import java.util.Map;

public record Rubric(
        String rubricId,
        String description,
        List<DimensionRef> usesDimensions,
        Map<String, List<String>> perTurnRules,
        Map<String, LevelExpectation> levelExpectations
) {

    public record LevelExpectation(
            List<String> mustReach2,
            List<String> mustReach3,
            boolean mustReach1All
    ) {}

    public List<String> selectDimensions() {
        List<String> answerDims = perTurnRules.get("on_intent_answer");
        if (answerDims != null) {
            return answerDims;
        }
        return usesDimensions.stream().map(DimensionRef::ref).toList();
    }

    public LevelExpectation expectationFor(InterviewLevel level) {
        if (level == null || levelExpectations == null) {
            return null;
        }
        return levelExpectations.get(level.name().toLowerCase());
    }
}
