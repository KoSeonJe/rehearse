package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.ReplanResponse;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.domain.resume.service.ResumeReplanLoader.ReplanContext;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumePlanPreparationService {

    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final ResumeIngestionService resumeIngestionService;
    private final ResumeSkeletonPersister skeletonStore;
    private final ResumeInterviewPlanner resumeInterviewPlanner;
    private final InterviewPlanPersister planStore;
    private final ResumeReplanLoader replanLoader;

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

        InterviewPlan plan = generatePlan(skeleton, resolveDuration(durationMinutes), interviewId);
        planStore.save(interviewId, plan);
        return plan;
    }

    private int resolveDuration(Integer durationMinutes) {
        return durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
    }

    public ReplanResponse replan(Long interviewId, Long userId) {
        ReplanContext context = replanLoader.load(interviewId, userId);

        InterviewPlan freshPlan = generatePlan(
                context.skeleton(),
                resolveDuration(context.durationMinutes()),
                interviewId);

        boolean replaced = planStore.findByInterviewId(interviewId).isPresent();
        InterviewPlan saved = planStore.replace(interviewId, freshPlan);

        log.info("interview replan 완료 — interviewId={}, planId={}, replaced={}",
                interviewId, saved.getId(), replaced);
        return ReplanResponse.of(interviewId, saved.getId(), replaced);
    }

    private InterviewPlan generatePlan(ResumeSkeleton skeleton, int durationMinutes, Long interviewId) {
        try {
            return resumeInterviewPlanner.plan(skeleton, durationMinutes);
        } catch (BusinessException e) {
            if (isAiInfraFailure(e)) {
                log.warn("planner LLM 인프라 실패 — interviewId={}, code={}, message={}",
                        interviewId, e.getCode(), e.getMessage());
                throw new BusinessException(ResumePlannerErrorCode.PLAN_GENERATION_FAILED);
            }
            throw e;
        } catch (RuntimeException e) {
            log.warn("planner 호출 실패 — interviewId={}, cause={}", interviewId, e.getMessage(), e);
            throw new BusinessException(ResumePlannerErrorCode.PLAN_GENERATION_FAILED);
        }
    }

    private boolean isAiInfraFailure(BusinessException e) {
        String code = e.getCode();
        return code != null && code.startsWith("AI_");
    }
}
