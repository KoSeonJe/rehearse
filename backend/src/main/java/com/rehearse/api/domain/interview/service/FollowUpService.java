package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.service.InterviewPlanRuntimeCache;
import com.rehearse.api.domain.resume.service.ResumeSkeletonRuntimeCache;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.domain.resume.service.InterviewPlanPersister;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final InterviewRuntimeStateCache runtimeStateStore;
    private final AiCallMetrics aiCallMetrics;
    private final ResumeInterviewOrchestrator resumeOrchestrator;
    private final ResumeSkeletonPersister resumeSkeletonStore;
    private final InterviewPlanPersister interviewPlanStore;
    private final ResumeSkeletonRuntimeCache resumeSkeletonCache;
    private final InterviewPlanRuntimeCache interviewPlanCache;
    private final InterviewFinder interviewFinder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, Long userId, FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }

        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, userId, request.getQuestionSetId());
        InterviewRuntimeState state = runtimeStateStore.getOrInit(id, () -> new InterviewRuntimeState(context.level().name(), null));

        if (isResumeTrack(id, state)) {
            return delegateToResumeOrchestrator(id, request);
        }

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

        publishTurnCompletedEvent(id, context, turn, saveResult.question().getId());

        return buildAnswerResponse(stepB, saveResult.question(), exhausted);
    }

    private void publishTurnCompletedEvent(Long interviewId, FollowUpContext context,
                                            TurnAnalysisResult turn, Long questionId) {
        try {
            int turnIndex = context.nextOrderIndex() - 1;
            Interview interview = interviewFinder.findById(interviewId);
            TurnCompletedEvent event = TurnCompletedEvent.ofStandard(
                    interviewId, (long) turnIndex, interview.getUserId(),
                    questionId, context.questionSetId(),
                    turn.answerText(), turn.answerAnalysis(),
                    turn.intent().type(), context.level()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("TurnCompletedEvent 발행 실패 — 턴 진행 차단하지 않음: interviewId={}, reason={}",
                    interviewId, e.getMessage());
        }
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

    private boolean isResumeTrack(Long interviewId, InterviewRuntimeState state) {
        if (state == null) {
            return false;
        }
        if (state.getResumeSkeletonCache() != null) {
            return true;
        }
        // Caffeine evict 후 재초기화된 state는 skeleton=null → Interview 엔티티로 1차 판정
        Interview interview = interviewFinder.findById(interviewId);
        if (!interview.getInterviewTypes().contains(InterviewType.RESUME_BASED)) {
            return false;
        }
        // resume 트랙: skeleton 재로드 후 state에 다시 캐시
        resumeSkeletonStore.findByInterviewId(interviewId).ifPresent(skeleton ->
                runtimeStateStore.update(interviewId, s -> s.setResumeSkeleton(skeleton))
        );
        return true;
    }

    private FollowUpResponse delegateToResumeOrchestrator(Long interviewId, FollowUpRequest request) {
        InterviewRuntimeState state = runtimeStateStore.get(interviewId);
        ResumeSkeleton skeleton = state.getResumeSkeletonCache();

        InterviewPlan plan = state.getInterviewPlanCache();
        if (plan == null) {
            plan = interviewPlanStore.findByInterviewId(interviewId)
                    .orElseThrow(() -> new BusinessException(ResumeErrorCode.PLAN_NOT_FOUND));
            InterviewPlan finalPlan = plan;
            runtimeStateStore.update(interviewId, s -> s.setInterviewPlan(finalPlan));
        }

        Interview interview = interviewFinder.findById(interviewId);
        int durationMinutes = interview.getDurationMinutes();

        return resumeOrchestrator.processUserTurn(
                interviewId, durationMinutes,
                request.getQuestionContent(), request.getAnswerText(),
                request.getPreviousExchanges(), skeleton, plan
        );
    }
}
