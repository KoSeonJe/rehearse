package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.resume.service.ResumeQuestionPersister;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
    private final ResumeQuestionPersister resumeQuestionPersister;
    private final ResumeQuestionGenerator resumeQuestionGenerator;

    public void initiate(Long interviewId, String resumeFileHash, byte[] resumePdfBytes,
                         Integer durationMinutes, Position position, TechStack techStack) {
        try {
            int minutes = durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
            int mainCount = Math.max(MIN_MAIN_COUNT, Math.min(MAX_MAIN_COUNT, minutes / 3 + 2));

            log.info("[ResumeTrackInitiator] 이력서 질문 생성 시작: interviewId={}, fileHash={}, pdfBytes={}, openerCount={}, mainCount={}",
                    interviewId, resumeFileHash, resumePdfBytes == null ? 0 : resumePdfBytes.length,
                    OPENER_COUNT, mainCount);

            GeneratedResumeQuestions generated = resumeQuestionGenerator.generate(
                    resumePdfBytes, OPENER_COUNT, mainCount, position, techStack);
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
                    opener.bestAnswer(), order++, null));
        }
        for (var main : generated.mains()) {
            if (main.depthType() == null) {
                log.error("[ResumeTrackInitiator] main 질문 depthType 누락: interviewId={}, question={}",
                        interviewId, main.question());
                throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
            }
            drafts.add(new ResumeQuestionPersister.ResumeQuestionDraft(
                    QuestionType.RESUME_MAIN, main.question(), main.ttsQuestion(),
                    main.bestAnswer(), order++, main.depthType()));
        }
        resumeQuestionPersister.persistAll(interviewId, drafts);
        log.info("[ResumeTrackInitiator] 질문 적재 완료: interviewId={}, openers={}, mains={}",
                interviewId, generated.openers().size(), generated.mains().size());
    }
}
