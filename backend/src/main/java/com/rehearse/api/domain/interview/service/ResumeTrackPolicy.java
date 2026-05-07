package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.QuestionSet;
import org.springframework.stereotype.Component;

@Component
public class ResumeTrackPolicy implements InterviewTurnPolicy {

    // FollowUpContext 응답 표시용 상한. RESUME 트랙 실제 종료 = ChainStateTracker / ResumeModeTransitionPolicy / ClockWatcher 일임.
    static final int HARD_TURN_CAP = 7;

    @Override
    public InterviewTrack getTrack() {
        return InterviewTrack.RESUME;
    }

    @Override
    public int getMaxFollowUpRounds() {
        return HARD_TURN_CAP;
    }

    // RESUME 트랙 종료 = ChainStateTracker / ResumeModeTransitionPolicy / ClockWatcher 일임. follow-up cap = 무의미하므로 no-op.
    @Override
    public void assertCanContinue(Interview interview, QuestionSet questionSet) {
    }
}
