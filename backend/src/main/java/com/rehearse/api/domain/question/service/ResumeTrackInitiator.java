package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.InterviewPlanPersister;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.service.ResumePlanPreparationService;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResumeTrackInitiator {

    private static final int DEFAULT_DURATION_MIN = 30;

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final ResumePlanPreparationService resumePlanPreparationService;
    private final ResumeSkeletonPersister resumeSkeletonPersister;
    private final InterviewPlanPersister interviewPlanPersister;
    private final ResumeInterviewOrchestrator resumeInterviewOrchestrator;

    public void initiate(Long interviewId, String resumeFileHash, String resumeText, Integer durationMinutes) {
        resumePlanPreparationService.prepare(interviewId, resumeFileHash, resumeText, durationMinutes);

        ResumeSkeleton skeleton = resumeSkeletonPersister.findByInterviewId(interviewId)
                .orElseThrow(() -> new IllegalStateException("ResumeSkeleton not found after prepare: interviewId=" + interviewId));
        InterviewPlan plan = interviewPlanPersister.findByInterviewId(interviewId)
                .orElseThrow(() -> new IllegalStateException("InterviewPlan not found after prepare: interviewId=" + interviewId));

        int duration = durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MIN;
        resumeInterviewOrchestrator.startSession(interviewId, duration, skeleton, plan);

        transactionHandler.saveResults(interviewId, List.of());
    }
}
