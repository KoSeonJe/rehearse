package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import org.springframework.stereotype.Component;

@Component
public class ResumeRoutePolicy {

    public boolean isResumeTrack(InterviewRuntimeState state, Interview interview) {
        if (state != null && state.getResumeSkeletonCache() != null) {
            return true;
        }
        return interview.getInterviewTypes().contains(InterviewType.RESUME_BASED);
    }
}
