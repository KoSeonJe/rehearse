package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.resume.service.ResumeFinder;
import com.rehearse.api.domain.resume.service.ResumeInterviewService;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
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
    private final FollowUpTransactionHandler followUpTransactionHandler;
    private final InterviewRuntimeStateCache runtimeStateStore;
    private final InterviewFinder interviewFinder;
    private final ResumeFinder resumeFinder;
    private final ResumeInterviewService resumeInterviewService;
    private final ResumeRoutePolicy resumeRoutePolicy;
    private final FollowUpSkipHandler followUpSkipHandler;
    private final FollowUpResponseBuilder followUpResponseBuilder;

    @Transactional(propagation = NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, Long userId, FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }

        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, userId, request.getQuestionSetId());
        InterviewRuntimeState state = runtimeStateStore.getOrInit(id, () -> new InterviewRuntimeState(context.level().name(), null));
        Interview interview = interviewFinder.findById(id);

        if (resumeRoutePolicy.isResumeTrack(state, interview)) {
            return delegateToResumeFlow(id, interview, request);
        }

        AskedPerspectives askedPerspectives = AskedPerspectives.from(request.getPreviousExchanges());
        TurnAnalysisResult turn = audioTurnAnalyzer.analyze(
                id, resolveTurnId(context), audioFile,
                request.getQuestionContent(), context.mainReferenceType(), askedPerspectives);

        if (turn.answerAnalysis().recommendedNextAction() == RecommendedNextAction.SKIP) {
            return followUpSkipHandler.handleAnalyzerSkip(id, context, request, turn);
        }
        return generateAndSaveFollowUp(id, context, request, turn, askedPerspectives);
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
            return followUpSkipHandler.handleStepBSkip(id, context, request, turn, stepB.skipReason());
        }

        FollowUpSaveResult saveResult = followUpTransactionHandler.saveFollowUpResultAndPublishEvent(
                id, context, stepB, turn);
        boolean exhausted = saveResult.newFollowUpCount() >= context.maxFollowUpRounds();

        log.info("REALTIME 후속 질문 생성 완료(v3): interviewId={}, questionSetId={}, questionId={}, type={}, perspective={}, targetClaim={}, exhausted={}",
                id, request.getQuestionSetId(), saveResult.question().getId(),
                stepB.type(), stepB.selectedAnswerFeedbackPerspective(),
                stepB.targetClaimIdx(), exhausted);

        return followUpResponseBuilder.buildAnswerResponse(stepB, saveResult.question(), exhausted);
    }

    private static Long resolveTurnId(FollowUpContext context) {
        if (context.currentMainQuestionId() != null) {
            return context.currentMainQuestionId();
        }
        return (long) context.nextOrderIndex();
    }

    private FollowUpResponse delegateToResumeFlow(Long interviewId, Interview interview, FollowUpRequest request) {
        ResumeSkeleton skeleton = resolveResumeSkeleton(interviewId);
        int durationMinutes = interview.getDurationMinutes();
        InterviewPlan plan = resumeFinder.findInterviewPlan(interviewId)
                .orElseGet(() -> resumeInterviewService.ensureInterviewPlan(interviewId, skeleton, durationMinutes));

        return resumeInterviewService.processUserTurn(
                interviewId, durationMinutes,
                request.getQuestionContent(), request.getAnswerText(),
                request.getPreviousExchanges(), skeleton, plan,
                request.isTerminate()
        );
    }

    private ResumeSkeleton resolveResumeSkeleton(Long interviewId) {
        InterviewRuntimeState state = runtimeStateStore.get(interviewId);
        if (state != null && state.getResumeSkeletonCache() != null) {
            return state.getResumeSkeletonCache();
        }

        ResumeSkeleton skeleton = resumeFinder.findSkeletonByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(ResumeErrorCode.RESUME_PLAN_NOT_READY));
        runtimeStateStore.update(interviewId, s -> s.setResumeSkeleton(skeleton));
        return skeleton;
    }
}
