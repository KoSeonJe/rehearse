package com.rehearse.api.domain.interview;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.TurnAnalysis;
import com.rehearse.api.domain.interview.vo.IntentType;

import java.util.List;

public record AnswerAnalysis(
        long turnId,
        List<Claim> claims,
        List<Perspective> missingPerspectives,
        List<String> unstatedAssumptions,
        int answerQuality,
        RecommendedNextAction recommendedNextAction
) implements TurnAnalysis {

    @JsonCreator
    public AnswerAnalysis(
            @JsonProperty("turn_id") long turnId,
            @JsonProperty("claims") List<Claim> claims,
            @JsonProperty("missing_perspectives") List<Perspective> missingPerspectives,
            @JsonProperty("unstated_assumptions") List<String> unstatedAssumptions,
            @JsonProperty("answer_quality") int answerQuality,
            @JsonProperty("recommended_next_action") RecommendedNextAction recommendedNextAction
    ) {
        if (answerQuality < 1 || answerQuality > 5) {
            throw new IllegalArgumentException("AnswerAnalysis.answerQuality 는 1~5 범위여야 합니다: " + answerQuality);
        }
        if (recommendedNextAction == null) {
            throw new IllegalArgumentException("AnswerAnalysis.recommendedNextAction 는 null 일 수 없습니다.");
        }
        this.turnId = turnId;
        this.claims = claims != null ? List.copyOf(claims) : List.of();
        this.missingPerspectives = missingPerspectives != null ? List.copyOf(missingPerspectives) : List.of();
        this.unstatedAssumptions = unstatedAssumptions != null ? List.copyOf(unstatedAssumptions) : List.of();
        this.answerQuality = answerQuality;
        this.recommendedNextAction = recommendedNextAction;
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

    // L1 분류기가 OFF_TOPIC/CLARIFY 를 ANSWER 로 놓친 False Negative 안전망.
    // claims=[] AND quality≤1 + 비-CLARIFICATION 권고 → CLARIFICATION 으로 강제.
    public AnswerAnalysis applyL1FalseNegativeGuard(IntentType intentType) {
        if (intentType != IntentType.ANSWER) {
            return this;
        }
        boolean noClaims = claims.isEmpty();
        boolean lowQuality = answerQuality <= 1;
        if (noClaims && lowQuality && recommendedNextAction != RecommendedNextAction.CLARIFICATION) {
            return withRecommendedNextAction(RecommendedNextAction.CLARIFICATION);
        }
        return this;
    }
}
