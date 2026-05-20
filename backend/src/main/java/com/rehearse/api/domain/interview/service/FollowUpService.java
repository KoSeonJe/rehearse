package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.BlockReason;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpService {

    private final AudioTurnAnalyzer audioTurnAnalyzer;
    private final FollowUpQuestionService followUpQuestionService;
    private final FollowUpTransactionHandler followUpTransactionHandler;
    private final StandardFollowUpPolicy standardFollowUpPolicy;
    private final ResumeSkeletonPersister resumeSkeletonStore;
    private final AiCallMetrics aiCallMetrics;

    @Transactional(propagation = NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, Long userId, FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }

        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, userId, request.getQuestionSetId());

        AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
                id, audioFile, request.getQuestionContent(), context.mainReferenceType());
        String answerText = request.getAnswerText();

        followUpTransactionHandler.publishAnswerAnalysisCompletedEvent(
                id, context, analysis, answerText, context.currentMainQuestionId());

        if (context.currentMainQuestionType() == QuestionType.RESUME_OPENER) {
            log.info("RESUME_OPENER → follow-up 생성 skip. interviewId={}, questionSetId={}",
                    id, request.getQuestionSetId());
            aiCallMetrics.incrementFollowUpSkip("opener_skip");
            return FollowUpResponse.aiSkip(answerText, "resume_opener_skip");
        }
        if (analysis.recommendedNextAction() == RecommendedNextAction.SKIP) {
            return handleAnalyzerSkip(id, context, request, analysis, answerText);
        }
        return generateAndSaveFollowUp(id, context, request, analysis, answerText);
    }

    private FollowUpResponse handleAnalyzerSkip(
            Long id, FollowUpContext context, FollowUpRequest request,
            AnswerAnalysis analysis, String answerText
    ) {
        log.info("Analyzer SKIP 권고 → Step B 미호출. interviewId={}, questionSetId={}",
                id, request.getQuestionSetId());
        aiCallMetrics.incrementFollowUpSkip("analyzer_skip");
        int turnIndex = request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size();
        log.warn("[진행차단진단] interviewId={} track={} stage=followup reason={} turnIndex={}",
                id, InterviewTrack.CS.logLabel(),
                BlockReason.ANALYZER_SKIP.logValue(), turnIndex);
        return FollowUpResponse.aiSkip(answerText, "analyzer_recommend_skip");
    }

    private FollowUpResponse generateAndSaveFollowUp(
            Long id, FollowUpContext context, FollowUpRequest request,
            AnswerAnalysis analysis, String answerText
    ) {
        ResumeSkeleton skeleton = resumeSkeletonStore.findByInterviewId(id).orElse(null);

        GeneratedFollowUp stepB = followUpQuestionService.write(
                request.getQuestionContent(), answerText, analysis, skeleton);

        if (stepB.isSkipped()) {
            log.info("Step B 가 skip 반환: interviewId={}, questionSetId={}, reason={}",
                    id, request.getQuestionSetId(), stepB.skipReason());
            aiCallMetrics.incrementFollowUpSkip("step_b_skip");
            int turnIndex = request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size();
            log.warn("[진행차단진단] interviewId={} track={} stage=followup reason={} turnIndex={}",
                    id, InterviewTrack.CS.logLabel(),
                    BlockReason.STEP_B_SKIP.logValue(), turnIndex);
            return FollowUpResponse.aiSkip(answerText, stepB.skipReason());
        }

        FollowUpSaveResult saveResult = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), stepB);
        boolean exhausted = saveResult.newFollowUpCount() >= context.maxFollowUpRounds();

        log.info("REALTIME 후속 질문 생성 완료: interviewId={}, questionSetId={}, questionId={}, type={}, weakestDimension={}, dimensionGaps={}, target_claim_idx={}, exhausted={}",
                id, request.getQuestionSetId(), saveResult.question().getId(),
                stepB.type(), analysis.weakestDimension(), analysis.dimensionGaps(),
                stepB.targetClaimIdx(), exhausted);

        return buildAnswerResponse(stepB, saveResult.question(), exhausted);
    }

    private static FollowUpResponse buildAnswerResponse(GeneratedFollowUp followUp, Question savedQuestion, boolean exhausted) {
        return FollowUpResponse.builder()
                .questionId(savedQuestion.getId())
                .question(followUp.question())
                .ttsQuestion(followUp.ttsQuestion())
                .reason(followUp.reason())
                .type(followUp.type())
                .answerText(followUp.answerText())
                .bestAnswer(savedQuestion.getBestAnswer())
                .skip(false)
                .presentToUser(true)
                .followUpExhausted(exhausted)
                .build();
    }
}
