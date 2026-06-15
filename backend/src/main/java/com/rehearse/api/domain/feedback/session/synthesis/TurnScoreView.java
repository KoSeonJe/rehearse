package com.rehearse.api.domain.feedback.session.synthesis;

public record TurnScoreView(
        String turnLabel,
        String verbalComment,
        String accuracyIssues,
        String coachingStructure,
        String coachingImprovement,
        String nonverbalComment,
        String vocalComment,
        String attitudeComment,
        String overallComment
) {
}
