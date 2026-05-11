package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.repository.InterviewPlanRepository;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
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

    private final ResumeSkeletonRepository resumeSkeletonRepository;
    private final ResumeSkeletonCodec resumeSkeletonCodec;
    private final InterviewPlanRepository interviewPlanRepository;
    private final InterviewPlanRuntimeCache interviewPlanRuntimeCache;

    public Optional<ResumeSkeleton> findSkeletonByInterviewId(Long interviewId) {
        return resumeSkeletonRepository.findByInterviewId(interviewId)
                .map(resumeSkeletonCodec::deserialize);
    }

    public Optional<InterviewPlan> findInterviewPlan(Long interviewId) {
        InterviewPlan cached = interviewPlanRuntimeCache.read(interviewId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return interviewPlanRepository.findByInterviewId(interviewId);
    }
}
