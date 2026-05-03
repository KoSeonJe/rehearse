package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewPlanRuntimeCache {

    private final InterviewRuntimeStateCache runtimeStateStore;

    public InterviewPlan read(Long interviewId) {
        return runtimeStateStore.get(interviewId).getInterviewPlanCache();
    }

    public void write(Long interviewId, InterviewPlan plan) {
        try {
            runtimeStateStore.update(interviewId, state -> state.setInterviewPlan(plan));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "Runtime state must be seeded before interview plan cache write: interviewId=" + interviewId, e);
        }
    }
}
