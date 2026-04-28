package com.rehearse.api.domain.feedback.rubric.event;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import org.springframework.lang.Nullable;

public record TurnCompletedEvent(
        Long interviewId,
        Long turnIndex,
        Long userId,
        Long questionId,
        Long questionSetId,
        String userAnswer,
        AnswerAnalysis analysis,
        IntentType intent,
        InterviewLevel userLevel,
        @Nullable ResumeMode resumeMode,
        @Nullable Integer currentChainLevel,
        @Nullable ResumeSkeleton resumeSkeleton
) {

    public static TurnCompletedEvent ofStandard(
            Long interviewId, Long turnIndex, Long userId,
            Long questionId, Long questionSetId,
            String userAnswer, AnswerAnalysis analysis,
            IntentType intent, InterviewLevel userLevel
    ) {
        return new TurnCompletedEvent(
                interviewId, turnIndex, userId,
                questionId, questionSetId,
                userAnswer, analysis, intent, userLevel,
                null, null, null
        );
    }

    public static TurnCompletedEvent ofResumeTrack(
            Long interviewId, Long turnIndex, Long userId,
            Long questionId, Long questionSetId,
            String userAnswer, AnswerAnalysis analysis,
            IntentType intent, InterviewLevel userLevel,
            ResumeMode resumeMode, Integer currentChainLevel,
            ResumeSkeleton resumeSkeleton
    ) {
        return new TurnCompletedEvent(
                interviewId, turnIndex, userId,
                questionId, questionSetId,
                userAnswer, analysis, intent, userLevel,
                resumeMode, currentChainLevel, resumeSkeleton
        );
    }
}
