package com.rehearse.api.domain.interview.event;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;

public record AnswerAnalysisCompletedEvent(
        Long interviewId,
        Long userId,
        Long questionId,
        Long questionSetId,
        AnswerAnalysis analysis,
        InterviewLevel userLevel
) {

    public static AnswerAnalysisCompletedEvent of(
            Long interviewId, Long userId,
            Long questionId, Long questionSetId,
            AnswerAnalysis analysis,
            InterviewLevel userLevel
    ) {
        return new AnswerAnalysisCompletedEvent(
                interviewId, userId,
                questionId, questionSetId,
                analysis, userLevel
        );
    }
}
