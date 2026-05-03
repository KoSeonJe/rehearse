package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumePlanPreparationService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final ResumeIngestionService resumeIngestionService;
    private final ResumeSkeletonPersister skeletonStore;
    private final ResumeInterviewPlanner resumeInterviewPlanner;
    private final InterviewPlanPersister planStore;

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
}
