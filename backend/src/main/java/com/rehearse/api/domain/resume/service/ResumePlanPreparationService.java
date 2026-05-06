package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.ReplanResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumePlanPreparationService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final ResumeIngestionService resumeIngestionService;
    private final ResumeSkeletonPersister skeletonStore;
    private final ResumeInterviewPlanner resumeInterviewPlanner;
    private final InterviewPlanPersister planStore;
    private final InterviewFinder interviewFinder;

    public PreparedResume prepare(Long interviewId, String resumeFileHash, String normalizedResumeText, Integer durationMinutes) {
        ResumeSkeleton skeleton = resolveSkeleton(interviewId, resumeFileHash, normalizedResumeText);
        InterviewPlan plan = resolvePlan(interviewId, skeleton, durationMinutes);
        return new PreparedResume(skeleton, plan);
    }

    private ResumeSkeleton resolveSkeleton(Long interviewId, String resumeFileHash, String normalizedResumeText) {
        ResumeSkeleton persisted = skeletonStore.findByInterviewId(interviewId).orElse(null);
        if (persisted != null) {
            if (resumeFileHash == null || resumeFileHash.equals(persisted.fileHash())) {
                return persisted;
            }
        }

        if (normalizedResumeText == null || resumeFileHash == null) {
            throw new BusinessException(ResumeErrorCode.RESUME_PLAN_NOT_READY);
        }
        return resumeIngestionService.ingestExtractedText(interviewId, normalizedResumeText, resumeFileHash);
    }

    private InterviewPlan resolvePlan(Long interviewId, ResumeSkeleton skeleton, Integer durationMinutes) {
        InterviewPlan persisted = planStore.findByInterviewId(interviewId).orElse(null);
        if (persisted != null) {
            return persisted;
        }

        InterviewPlan plan = resumeInterviewPlanner.plan(skeleton, resolveDuration(durationMinutes));
        planStore.save(interviewId, plan);
        return plan;
    }

    private int resolveDuration(Integer durationMinutes) {
        return durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
    }

    @Transactional
    public ReplanResponse replan(Long interviewId, Long userId) {
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

        boolean replaced = planStore.findByInterviewId(interviewId).isPresent();
        InterviewPlan freshPlan = resumeInterviewPlanner.plan(skeleton, resolveDuration(interview.getDurationMinutes()));
        InterviewPlan saved = planStore.replace(interviewId, freshPlan);

        log.info("interview replan 완료 — interviewId={}, planId={}, replaced={}",
                interviewId, saved.getId(), replaced);
        return ReplanResponse.of(interviewId, saved.getId(), replaced);
    }
}
