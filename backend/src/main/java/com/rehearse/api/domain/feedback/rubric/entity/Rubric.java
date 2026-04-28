package com.rehearse.api.domain.feedback.rubric.entity;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.resume.entity.ResumeMode;

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

    public List<String> selectDimensions(IntentType intent, ResumeMode resumeMode) {
        if (intent == IntentType.CLARIFY_REQUEST) {
            List<String> dims = perTurnRules.get("on_intent_clarify");
            return dims != null ? dims : List.of();
        }
        if (intent == IntentType.GIVE_UP) {
            List<String> dims = perTurnRules.get("on_intent_give_up");
            return dims != null ? dims : List.of();
        }

        if (resumeMode != null) {
            String modeKey = switch (resumeMode) {
                case PLAYGROUND -> "on_playground_mode";
                case INTERROGATION -> "on_interrogation_mode";
                case WRAP_UP -> "on_wrap_up_mode";
            };
            List<String> modeDims = perTurnRules.get(modeKey);
            if (modeDims != null) {
                return modeDims;
            }
        }

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
