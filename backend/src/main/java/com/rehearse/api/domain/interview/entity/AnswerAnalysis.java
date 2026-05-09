package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.entity.TurnAnalysis;

import java.util.List;

public record AnswerAnalysis(
        long turnId,
        List<Claim> claims,
        List<AnswerFeedbackPerspective> missingPerspectives,
        List<String> unstatedAssumptions,
        int answerQuality,
        RecommendedNextAction recommendedNextAction
) implements TurnAnalysis {

    public AnswerAnalysis {
        claims = claims != null ? List.copyOf(claims) : List.of();
        missingPerspectives = missingPerspectives != null ? List.copyOf(missingPerspectives) : List.of();
        unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
    }

    public AnswerAnalysis withRecommendedNextAction(RecommendedNextAction newAction) {
        return new AnswerAnalysis(turnId, claims, missingPerspectives, unstatedAssumptions, answerQuality, newAction);
    }

    public AnswerAnalysis withTurnId(long newTurnId) {
        return new AnswerAnalysis(newTurnId, claims, missingPerspectives, unstatedAssumptions, answerQuality, recommendedNextAction);
    }

    public static AnswerAnalysis empty(long turnId) {
        return new AnswerAnalysis(turnId, List.of(), List.of(), List.of(), 1, RecommendedNextAction.CLARIFICATION);
    }

    public AnswerAnalysis applyL1FalseNegativeGuard() {
        boolean noClaims = claims.isEmpty();
        boolean lowQuality = answerQuality <= 1;
        if (noClaims && lowQuality && recommendedNextAction != RecommendedNextAction.CLARIFICATION) {
            return withRecommendedNextAction(RecommendedNextAction.CLARIFICATION);
        }
        return this;
    }
}
