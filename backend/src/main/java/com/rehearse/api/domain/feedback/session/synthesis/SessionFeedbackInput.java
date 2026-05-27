package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.interview.entity.InterviewLevel;

import java.util.List;
import java.util.Map;

public record SessionFeedbackInput(
        SessionMetadata sessionMetadata,
        List<TurnScoreView> turnScores,
        Map<String, Map<String, Double>> scoresByCategory,
        List<String> appliedRubrics,
        String deliveryAnalysis,
        String visionAnalysis,
        NonverbalDeliveryAggregate nonverbalAggregate,
        String legacyNonverbalAggregateJson,
        String coverage,
        InterviewLevel userLevel
) {
    public record SessionMetadata(
            Long interviewId,
            String position,
            String level,
            List<String> interviewTypes,
            int totalTurns,
            int durationMinutes
    ) {
    }

    public record NonverbalDeliveryAggregate(
            String source,
            List<NonverbalTurnAggregate> turns,
            Map<String, Double> averageScores,
            LowestDimension lowestDimension,
            double averageContextMultiplier,
            List<RecommendedAction> recommendedActions
    ) {
    }

    public record NonverbalTurnAggregate(
            String turnLabel,
            Map<String, Integer> scores,
            double contextMultiplier
    ) {
    }

    public record LowestDimension(
            String dimension,
            double averageScore
    ) {
    }

    public record RecommendedAction(
            String dimension,
            List<String> actions
    ) {
    }
}
