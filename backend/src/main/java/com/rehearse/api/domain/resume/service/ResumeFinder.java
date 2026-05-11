package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeFinder {

    private final ResumeSkeletonPersister resumeSkeletonPersister;
    private final InterviewPlanPersister interviewPlanPersister;
    private final InterviewPlanRuntimeCache interviewPlanRuntimeCache;

    public Optional<ResumeSkeleton> findSkeletonByInterviewId(Long interviewId) {
        return resumeSkeletonPersister.findByInterviewId(interviewId);
    }

    public Optional<InterviewPlan> findInterviewPlan(Long interviewId) {
        InterviewPlan cached = interviewPlanRuntimeCache.read(interviewId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return interviewPlanPersister.findByInterviewId(interviewId);
    }
}
