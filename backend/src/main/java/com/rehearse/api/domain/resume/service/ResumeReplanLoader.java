package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeReplanLoader {

    private final InterviewFinder interviewFinder;
    private final ResumeSkeletonPersister skeletonStore;

    @Transactional(readOnly = true)
    public ReplanContext load(Long interviewId, Long userId) {
        Interview interview = interviewFinder.findById(interviewId);
        interview.validateOwner(userId);

        if (!interview.getInterviewTypes().contains(InterviewType.RESUME_BASED)) {
            log.warn("replan 차단 — RESUME_BASED 아님: interviewId={}, types={}",
                    interviewId, interview.getInterviewTypes());
            throw new BusinessException(InterviewErrorCode.INTERVIEW_NOT_RESUME_BASED);
        }

        ResumeSkeleton skeleton = skeletonStore.findByInterviewId(interviewId)
                .orElseThrow(() -> {
                    log.warn("replan 차단 — skeleton 부재: interviewId={}", interviewId);
                    return new BusinessException(ResumeErrorCode.RESUME_PLAN_NOT_READY);
                });

        return new ReplanContext(skeleton, interview.getDurationMinutes());
    }

    public record ReplanContext(ResumeSkeleton skeleton, Integer durationMinutes) {
    }
}
