package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.service.PreparedResume;
import com.rehearse.api.domain.resume.service.ResumeInterviewService;
import com.rehearse.api.domain.resume.service.ResumePlanPreparationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ResumeTrackInitiator {

    private static final int DEFAULT_DURATION_MIN = 30;

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final ResumePlanPreparationService resumePlanPreparationService;
    private final ResumeInterviewService resumeInterviewService;
    private final InterviewRuntimeStateCache runtimeStateStore;

    public void initiate(Long interviewId, InterviewLevel level, String resumeFileHash, String resumeText, Integer durationMinutes) {
        PreparedResume prepared = resumePlanPreparationService.prepare(interviewId, resumeFileHash, resumeText, durationMinutes);

        String levelName = level != null ? level.name() : InterviewLevel.JUNIOR.name();
        runtimeStateStore.getOrInit(interviewId,
                () -> InterviewRuntimeState.seed(levelName, prepared.skeleton(), prepared.plan()));

        int duration = durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MIN;
        resumeInterviewService.startSession(interviewId, duration, prepared.skeleton(), prepared.plan());

        transactionHandler.saveResults(interviewId, List.of());
    }
}
