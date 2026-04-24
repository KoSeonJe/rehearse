package com.rehearse.api.domain.interview.policy;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.entity.QuestionSet;

public interface InterviewTurnPolicy {

    InterviewTrack getTrack();

    void assertCanContinue(Interview interview, QuestionSet questionSet);
}
