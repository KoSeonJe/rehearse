package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewTurnPolicyResolver {

    private final StandardFollowUpPolicy standardFollowUpPolicy;
    private final ResumeTrackPolicy resumeTrackPolicy;

    public InterviewTurnPolicy resolve(Interview interview) {
        return switch (interview.getTrack()) {
            case RESUME -> resumeTrackPolicy;
            case CS, LANGUAGE -> standardFollowUpPolicy;
        };
    }
}
