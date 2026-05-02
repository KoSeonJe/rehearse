package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeModeTransitionPolicy {

    private final InterviewRuntimeStateCache runtimeStateStore;

    @Value("${rehearse.resume-track.wrap-up-threshold-min:2}")
    private long wrapUpThresholdMin;

    @Value("${rehearse.resume-track.hard-timeout-min:10}")
    private long hardTimeoutMin;

    public ResumeMode advanceToWrapUpIfDue(Long interviewId, long remainingMinutes, ResumeMode current) {
        if (remainingMinutes <= wrapUpThresholdMin && current != ResumeMode.WRAP_UP) {
            log.info("[ResumeOrchestrator] WRAP_UP 전이: interviewId={}, remainingMin={}", interviewId, remainingMinutes);
            runtimeStateStore.update(interviewId, s -> s.transitionTo(ResumeMode.WRAP_UP));
            InterviewRuntimeState refreshed = runtimeStateStore.get(interviewId);
            return refreshed.getResumeMode();
        }
        return current;
    }

    public boolean isHardTimeoutExceeded(int durationMinutes, long remainingMinutes) {
        long elapsedMinutes = durationMinutes - remainingMinutes;
        return elapsedMinutes >= durationMinutes + hardTimeoutMin;
    }
}
