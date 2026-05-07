package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.QuestionSet;
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

    /**
     * RESUME 트랙 종료 제어는 ChainStateTracker / ResumeModeTransitionPolicy / ClockWatcher 가 보장한다.
     * follow-up count 기반 cap 가드는 RESUME 트랙에서 무의미하므로 no-op.
     */
    @Override
    public void assertCanContinue(Interview interview, QuestionSet questionSet) {
    }
}
