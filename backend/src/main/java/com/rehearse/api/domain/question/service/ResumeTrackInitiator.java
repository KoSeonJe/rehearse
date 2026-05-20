package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.ResumeIngestionService;
import com.rehearse.api.domain.resume.service.ResumeQuestionPersister;
import com.rehearse.api.domain.resume.service.ResumeSkeletonSampler;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeTrackInitiator {

    private static final int OPENER_COUNT = 1;
    private static final int MIN_MAIN_COUNT = 7;
    private static final int MAX_MAIN_COUNT = 40;
    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final ResumeIngestionService resumeIngestionService;
    private final ResumeQuestionPersister resumeQuestionPersister;
    private final ResumeSkeletonSampler resumeSkeletonSampler;
    private final ResumeQuestionGenerator resumeQuestionGenerator;

    public void initiate(Long interviewId, String resumeFileHash, byte[] resumePdfBytes, Integer durationMinutes) {
        try {
            ResumeSkeleton skeleton = resumeIngestionService.ingestPdf(interviewId, resumePdfBytes, resumeFileHash);

            int minutes = durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
            int mainCount = Math.max(MIN_MAIN_COUNT, Math.min(MAX_MAIN_COUNT, minutes / 3 + 2));

            ResumeSkeleton sampledSkeleton = resumeSkeletonSampler.sampleDecisions(skeleton, interviewId);
            GeneratedResumeQuestions generated = resumeQuestionGenerator.generate(sampledSkeleton, OPENER_COUNT, mainCount);
            persistGenerated(interviewId, generated);
            transactionHandler.completeGeneration(interviewId);
        } catch (Exception e) {
            log.error("[ResumeTrackInitiator] 면접 시작 실패: interviewId={}, reason={}",
                    interviewId, e.getMessage());
            transactionHandler.failGeneration(interviewId, "이력서 트랙 시작 실패: " + e.getMessage());
            throw e;
        }
    }

    private void persistGenerated(Long interviewId, GeneratedResumeQuestions generated) {
        List<ResumeQuestionPersister.ResumeQuestionDraft> drafts = new ArrayList<>(
                generated.openers().size() + generated.mains().size());
        int order = 0;
        for (var opener : generated.openers()) {
            drafts.add(new ResumeQuestionPersister.ResumeQuestionDraft(
                    QuestionType.RESUME_OPENER, opener.question(), opener.ttsQuestion(),
                    opener.bestAnswer(), order++));
        }
        for (var main : generated.mains()) {
            drafts.add(new ResumeQuestionPersister.ResumeQuestionDraft(
                    QuestionType.RESUME_MAIN, main.question(), main.ttsQuestion(),
                    main.bestAnswer(), order++));
        }
        resumeQuestionPersister.persistAll(interviewId, drafts);
        log.info("[ResumeTrackInitiator] 질문 적재 완료: interviewId={}, openers={}, mains={}",
                interviewId, generated.openers().size(), generated.mains().size());
    }
}
