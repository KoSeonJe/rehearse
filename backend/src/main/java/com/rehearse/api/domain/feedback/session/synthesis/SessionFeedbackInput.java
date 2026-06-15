package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.interview.entity.InterviewLevel;

import java.util.List;

public record SessionFeedbackInput(
        SessionMetadata sessionMetadata,
        List<TurnScoreView> turnScores,
        String deliveryAnalysis,
        String visionAnalysis,
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
}
