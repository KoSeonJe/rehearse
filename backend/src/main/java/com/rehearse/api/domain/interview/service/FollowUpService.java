package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.vo.TurnAnalysisResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.vo.AskedPerspectives;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
    private final FollowUpQuestionWriter followUpQuestionWriter;
    private final IntentDispatcher intentDispatcher;
    private final FollowUpTransactionHandler followUpTransactionHandler;
    private final InterviewRuntimeStateStore runtimeStateStore;
    private final AiCallMetrics aiCallMetrics;

    @Transactional(propagation = NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, Long userId, FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }

        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, userId, request.getQuestionSetId());
        runtimeStateStore.getOrInit(id, () -> new InterviewRuntimeState(context.level().name(), null));

        AskedPerspectives askedPerspectives = AskedPerspectives.from(request.getPreviousExchanges());
        TurnAnalysisResult turn = audioTurnAnalyzer.analyze(
                id, resolveTurnId(context), audioFile,
                request.getQuestionContent(), context.mainReferenceType(), askedPerspectives);

        if (turn.intent().type() != IntentType.ANSWER) {
            return handleNonAnswerIntent(id, context, request, turn);
        }
        if (turn.answerAnalysis().recommendedNextAction() == RecommendedNextAction.SKIP) {
            return handleAnalyzerSkip(id, request, turn);
        }
        return generateAndSaveFollowUp(id, context, request, turn, askedPerspectives);
    }

    private FollowUpResponse handleNonAnswerIntent(
            Long id, FollowUpContext context, FollowUpRequest request, TurnAnalysisResult turn
    ) {
        IntentType intentType = turn.intent().type();
        log.info("[FollowUp] intent != ANSWER 분기: interviewId={}, questionSetId={}, intent={}, confidence={}",
                id, request.getQuestionSetId(), intentType, turn.intent().confidence());
        aiCallMetrics.incrementFollowUpSkip("intent_" + intentType.name().toLowerCase());

        int turnIndex = request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size();
        IntentBranchInput input = new IntentBranchInput(
                id, context, request.getQuestionContent(), turn.answerText(),
                turnIndex, request.getPreviousExchanges());
        return intentDispatcher.dispatch(intentType, input);
    }

    private FollowUpResponse handleAnalyzerSkip(Long id, FollowUpRequest request, TurnAnalysisResult turn) {
        log.info("Analyzer SKIP 권고 → Step B 미호출. interviewId={}, questionSetId={}",
                id, request.getQuestionSetId());
        aiCallMetrics.incrementFollowUpSkip("analyzer_skip");
        return FollowUpResponse.aiSkip(turn.answerText(), "analyzer_recommend_skip");
    }

    private FollowUpResponse generateAndSaveFollowUp(
            Long id, FollowUpContext context, FollowUpRequest request,
            TurnAnalysisResult turn, AskedPerspectives askedPerspectives
    ) {
        String answerText = turn.answerText();
        AnswerAnalysis analysis = turn.answerAnalysis();

        FollowUpGenerationRequest stepBReq = new FollowUpGenerationRequest(
                context.position(), context.effectiveTechStack(), context.level(),
                request.getQuestionContent(), answerText, request.getNonVerbalSummary(),
                request.getPreviousExchanges(), context.mainReferenceType());
        GeneratedFollowUp stepB = followUpQuestionWriter.write(stepBReq, analysis, askedPerspectives);

        if (stepB.isSkipped()) {
            log.info("Step B 가 skip 반환: interviewId={}, questionSetId={}, reason={}",
                    id, request.getQuestionSetId(), stepB.getSkipReason());
            aiCallMetrics.incrementFollowUpSkip("step_b_skip");
            return FollowUpResponse.aiSkip(answerText, stepB.getSkipReason());
        }
        ensureQuestionPresent(id, request.getQuestionSetId(), stepB);

        FollowUpSaveResult saveResult = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), stepB, context.nextOrderIndex());
        boolean exhausted = saveResult.newFollowUpCount() >= context.maxFollowUpRounds();

        log.info("REALTIME 후속 질문 생성 완료(v3): interviewId={}, questionSetId={}, questionId={}, type={}, perspective={}, targetClaim={}, exhausted={}",
                id, request.getQuestionSetId(), saveResult.question().getId(),
                stepB.getType(), stepB.getSelectedPerspective(),
                stepB.getTargetClaimIdx(), exhausted);

        return buildAnswerResponse(stepB, saveResult.question(), exhausted);
    }

    private static void ensureQuestionPresent(Long id, Long questionSetId, GeneratedFollowUp stepB) {
        if (stepB.getQuestion() == null || stepB.getQuestion().isBlank()) {
            log.warn("Step B 응답 스키마 위반: skip=false인데 question이 비어있음. interviewId={}, questionSetId={}",
                    id, questionSetId);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    private static Long resolveTurnId(FollowUpContext context) {
        if (context.currentMainQuestionId() != null) {
            return context.currentMainQuestionId();
        }
        return (long) context.nextOrderIndex();
    }

    private static FollowUpResponse buildAnswerResponse(GeneratedFollowUp followUp, Question savedQuestion, boolean exhausted) {
        return FollowUpResponse.builder()
                .questionId(savedQuestion.getId())
                .question(followUp.getQuestion())
                .ttsQuestion(followUp.getTtsQuestion())
                .reason(followUp.getReason())
                .type(followUp.getType())
                .answerText(followUp.getAnswerText())
                .modelAnswer(savedQuestion.getModelAnswer())
                .skip(false)
                .presentToUser(true)
                .followUpExhausted(exhausted)
                .selectedPerspective(followUp.getSelectedPerspective())
                .build();
    }
}
