package com.rehearse.api.domain.interview.policy;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.exception.QuestionErrorCode;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StandardFollowUpPolicy implements InterviewTurnPolicy {

    private final int maxRounds;

    public StandardFollowUpPolicy(
            @Value("${rehearse.interview.policy.standard.max-follow-up-rounds:2}") int maxRounds) {
        this.maxRounds = maxRounds;
    }

    @Override
    public InterviewTrack getTrack() {
        return InterviewTrack.CS;
    }

    @Override
    public void assertCanContinue(Interview interview, QuestionSet questionSet) {
        long followUpCount = questionSet.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
                .count();

        if (followUpCount >= maxRounds) {
            throw new BusinessException(QuestionErrorCode.MAX_FOLLOWUP_EXCEEDED);
        }
    }
}
