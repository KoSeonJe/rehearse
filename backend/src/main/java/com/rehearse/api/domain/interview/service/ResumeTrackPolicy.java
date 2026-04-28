package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.exception.QuestionErrorCode;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.global.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class ResumeTrackPolicy implements InterviewTurnPolicy {

    static final int HARD_TURN_CAP = 7;

    @Override
    public InterviewTrack getTrack() {
        return InterviewTrack.RESUME;
    }

    @Override
    public int getMaxFollowUpRounds() {
        return HARD_TURN_CAP;
    }

    @Override
    public void assertCanContinue(Interview interview, QuestionSet questionSet) {
        long followUpCount = questionSet.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
                .count();

        if (followUpCount >= HARD_TURN_CAP) {
            throw new BusinessException(QuestionErrorCode.MAX_FOLLOWUP_EXCEEDED);
        }
    }
}
