package com.rehearse.api.domain.resume.cache;

import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewPlanCache {

    private final InterviewRuntimeStateStore runtimeStateStore;

    public InterviewPlan read(Long interviewId) {
        try {
            return runtimeStateStore.get(interviewId).getInterviewPlanCache();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public void write(Long interviewId, InterviewPlan plan) {
        try {
            runtimeStateStore.update(interviewId, state -> state.setInterviewPlan(plan));
        } catch (IllegalStateException e) {
            log.warn("RuntimeState 미초기화로 인터뷰 플랜 캐시 갱신 스킵: interviewId={}", interviewId);
        }
    }
}
