package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.service.PreparedResume;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.service.ResumePlanPreparationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResumeTrackInitiator {

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final ResumePlanPreparationService resumePlanPreparationService;
    private final ResumeInterviewOrchestrator resumeInterviewOrchestrator;
    private final InterviewRuntimeStateCache runtimeStateStore;

    public void initiate(Long interviewId, InterviewLevel level, String resumeFileHash, String resumeText, Integer durationMinutes) {
        PreparedResume prepared = resumePlanPreparationService.prepare(interviewId, resumeFileHash, resumeText, durationMinutes);

        runtimeStateStore.getOrInit(interviewId,
                () -> InterviewRuntimeState.seed(level.name(), prepared.skeleton(), prepared.plan()));

        resumeInterviewOrchestrator.startSession(interviewId, durationMinutes, prepared.skeleton(), prepared.plan());

        transactionHandler.saveResults(interviewId, List.of());
    }
}
