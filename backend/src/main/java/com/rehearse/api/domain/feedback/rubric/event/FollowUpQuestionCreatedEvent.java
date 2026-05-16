package com.rehearse.api.domain.feedback.rubric.event;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;

public record FollowUpQuestionCreatedEvent(
        Long interviewId,
        Long userId,
        Long questionId,
        Long questionSetId,
        String userAnswer,
        AnswerAnalysis analysis,
        InterviewLevel userLevel
) {

    public static FollowUpQuestionCreatedEvent of(
            Long interviewId, Long userId,
            Long questionId, Long questionSetId,
            String userAnswer, AnswerAnalysis analysis,
            InterviewLevel userLevel
    ) {
        return new FollowUpQuestionCreatedEvent(
                interviewId, userId,
                questionId, questionSetId,
                userAnswer, analysis, userLevel
        );
    }
}
