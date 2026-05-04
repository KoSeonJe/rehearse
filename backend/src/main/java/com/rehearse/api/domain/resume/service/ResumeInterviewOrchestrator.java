package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.service.IntentDispatcher;
import com.rehearse.api.domain.interview.service.TurnAnalysisPipeline;
import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ChainStateTracker;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 이력서 면접 트랙 메인 진입점.
 * PLAYGROUND → INTERROGATION → WRAP_UP 3단계 FSM을 orchestrate한다.
 * LLM 호출은 트랜잭션 외부에서 수행한다 (@Transactional 제거 — 호출자가 NOT_SUPPORTED propagation으로 진입).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeInterviewOrchestrator {

    private final TurnAnalysisPipeline turnAnalysisPipeline;
    private final IntentDispatcher intentDispatcher;
    private final PlaygroundModeHandler playgroundHandler;
    private final InterrogationModeHandler interrogationHandler;
    private final WrapUpModeHandler wrapUpHandler;
    private final ClockWatcher clockWatcher;
    private final InterviewRuntimeStateCache runtimeStateStore;
    private final ResumeModeTransitionPolicy modeTransitionPolicy;
    private final ResumeTurnEventPublisher turnEventPublisher;
    private final QuestionSetRepository questionSetRepository;

    public FollowUpResponse processUserTurn(
            Long interviewId, int durationMinutes,
            String questionContent, String answerText,
            List<FollowUpExchange> previousExchanges,
            ResumeSkeleton skeleton, InterviewPlan plan
    ) {
        clockWatcher.markStart(interviewId);

        long turnIndex = previousExchanges != null ? previousExchanges.size() : 0;
        TurnAnalysisResult turnResult = turnAnalysisPipeline.analyze(
                interviewId, turnIndex, questionContent, answerText, previousExchanges);

        IntentResult intent = turnResult.intent();
        if (intent.type() != IntentType.ANSWER) {
            log.info("[ResumeOrchestrator] non-answer intent: interviewId={}, intent={}", interviewId, intent.type());
            return handleNonAnswerIntent(interviewId, questionContent, answerText, intent, previousExchanges);
        }

        AnswerAnalysis analysis = turnResult.answerAnalysis();
        InterviewRuntimeState state = runtimeStateStore.get(interviewId);
        long remainingMinutes = clockWatcher.remainingMinutes(interviewId, durationMinutes);

        ResumeMode currentMode = modeTransitionPolicy.advanceToWrapUpIfDue(
                interviewId, remainingMinutes, state.getResumeMode());

        if (currentMode == ResumeMode.WRAP_UP
                && modeTransitionPolicy.isHardTimeoutExceeded(durationMinutes, remainingMinutes)) {
            log.warn("[ResumeOrchestrator] hard timeout 초과 → 강제 종료: interviewId={}", interviewId);
            return hardTimeoutResponse();
        }

        InterviewRuntimeState currentState = runtimeStateStore.get(interviewId);
        ChainStateTracker chainTracker = currentState.getChainStateTracker();
        int currentChainLevel = chainTracker != null ? chainTracker.getCurrentLevel() : 1;

        TurnHandlerResult handlerResult = dispatchByMode(
                currentMode, interviewId, currentState, answerText, analysis,
                skeleton, plan, remainingMinutes, previousExchanges);

        turnEventPublisher.publish(interviewId, turnIndex, analysis, intent, currentMode,
                currentChainLevel, skeleton, answerText, handlerResult.questionId());

        return handlerResult.response();
    }

    public FollowUpResponse startSession(
            Long interviewId, int durationMinutes,
            ResumeSkeleton skeleton, InterviewPlan plan
    ) {
        java.util.Optional<com.rehearse.api.domain.question.entity.Question> existingOpener =
                questionSetRepository
                        .findByInterviewIdAndCategory(interviewId, QuestionSetCategory.RESUME_BASED)
                        .flatMap(qs -> qs.getQuestions().stream()
                                .filter(q -> q.getQuestionType() == QuestionType.RESUME_OPENER)
                                .findFirst());

        if (existingOpener.isPresent()) {
            com.rehearse.api.domain.question.entity.Question opener = existingOpener.get();
            log.info("[ResumeOrchestrator] 기존 RESUME_OPENER 재사용: interviewId={}", interviewId);
            return FollowUpResponse.builder()
                    .question(opener.getQuestionText())
                    .ttsQuestion(opener.getTtsText())
                    .presentToUser(true)
                    .type("RESUME_OPENER")
                    .build();
        }

        InterviewRuntimeState state = runtimeStateStore.get(interviewId);
        log.info("[ResumeOrchestrator] 세션 시작: interviewId={}, mode=PLAYGROUND", interviewId);
        PlaygroundModeHandler.OpenerResult openerResult = playgroundHandler.handleOpener(interviewId, state, skeleton, plan);
        return openerResult.response();
    }

    private TurnHandlerResult dispatchByMode(
            ResumeMode mode, Long interviewId, InterviewRuntimeState state,
            String answerText, AnswerAnalysis analysis,
            ResumeSkeleton skeleton, InterviewPlan plan, long remainingMinutes,
            List<FollowUpExchange> previousExchanges
    ) {
        return switch (mode) {
            case PLAYGROUND -> handlePlayground(interviewId, state, answerText, analysis, skeleton, plan, previousExchanges);
            case INTERROGATION -> {
                InterrogationModeHandler.InterrogationTurnResult r =
                        interrogationHandler.handle(interviewId, state, answerText, analysis, plan, previousExchanges);
                yield new TurnHandlerResult(r.response(), r.questionId());
            }
            case WRAP_UP -> {
                WrapUpModeHandler.WrapUpTurnResult r =
                        wrapUpHandler.handle(interviewId, state, answerText, analysis, remainingMinutes, true, previousExchanges);
                yield new TurnHandlerResult(r.response(), r.questionId());
            }
        };
    }

    private TurnHandlerResult handlePlayground(
            Long interviewId, InterviewRuntimeState state,
            String answerText, AnswerAnalysis analysis,
            ResumeSkeleton skeleton, InterviewPlan plan,
            List<FollowUpExchange> previousExchanges
    ) {
        PlaygroundModeHandler.PlaygroundTurnResult result =
                playgroundHandler.handle(interviewId, state, answerText, analysis, skeleton, plan, previousExchanges);

        if (result.switchedToInterrogation()) {
            runtimeStateStore.update(interviewId, s -> s.transitionTo(ResumeMode.INTERROGATION));
            InterviewRuntimeState refreshed = runtimeStateStore.get(interviewId);
            InterrogationModeHandler.InterrogationTurnResult interrogationResult =
                    interrogationHandler.handle(interviewId, refreshed, null, null, plan, previousExchanges);
            return new TurnHandlerResult(interrogationResult.response(), interrogationResult.questionId());
        }
        return new TurnHandlerResult(result.response(), result.questionId());
    }

    private FollowUpResponse hardTimeoutResponse() {
        return FollowUpResponse.builder()
                .followUpExhausted(true)
                .skip(true)
                .presentToUser(false)
                .type("RESUME_HARD_TIMEOUT")
                .build();
    }

    private FollowUpResponse handleNonAnswerIntent(
            Long interviewId, String questionContent, String answerText,
            IntentResult intent, List<FollowUpExchange> previousExchanges
    ) {
        int turnIndex = previousExchanges != null ? previousExchanges.size() : 0;
        IntentBranchInput input = new IntentBranchInput(
                interviewId, null, questionContent, answerText, turnIndex, previousExchanges
        );
        return intentDispatcher.dispatch(intent.type(), input);
    }

    private record TurnHandlerResult(FollowUpResponse response, Long questionId) {}
}
