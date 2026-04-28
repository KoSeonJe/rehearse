package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.interview.entity.InterviewLevel;

import java.util.List;
import java.util.Map;

public record SessionFeedbackInput(
        Object sessionMetadata,
        List<TurnScoreView> turnScores,
        Map<String, Map<String, Double>> scoresByCategory,
        List<String> appliedRubrics,
        String deliveryAnalysis,
        String visionAnalysis,
        String nonverbalAggregate,
        String coverage,
        InterviewLevel userLevel
) {
}
