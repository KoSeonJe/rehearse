package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.service.TurnAnalysisPipeline;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ChainStateTracker;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 이력서 면접 트랙 메인 진입점.
 * PLAYGROUND → INTERROGATION 2단계 FSM을 orchestrate한다.
 * LLM 호출은 트랜잭션 외부에서 수행한다 (@Transactional 제거 — 호출자가 NOT_SUPPORTED propagation으로 진입).
 * 종료 시점은 사용자 답변 액션에 동봉된 terminate 신호 또는 hard timeout backstop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeInterviewOrchestrator {

    private final TurnAnalysisPipeline turnAnalysisPipeline;
    private final PlaygroundModeHandler playgroundHandler;
    private final InterrogationModeHandler interrogationHandler;
    private final ClockWatcher clockWatcher;
    private final InterviewRuntimeStateCache runtimeStateStore;
    private final ResumeModeTransitionPolicy modeTransitionPolicy;
    private final ResumeTurnEventPublisher turnEventPublisher;
    private final QuestionSetRepository questionSetRepository;

    public FollowUpResponse processUserTurn(
            Long interviewId, int durationMinutes,
            String questionContent, String answerText,
            List<FollowUpExchange> previousExchanges,
            ResumeSkeleton skeleton, InterviewPlan plan,
            boolean terminate
    ) {
        try {
            return processUserTurnInternal(
                    interviewId, durationMinutes, questionContent, answerText,
                    previousExchanges, skeleton, plan, terminate);
        } catch (BusinessException e) {
            if (e.getErrorCode() == AiErrorCode.CONTEXT_BUDGET_EXCEEDED) {
                log.warn("[ResumeOrchestrator] 컨텍스트 토큰 예산 초과 → graceful 종료: interviewId={}", interviewId);
                return contextBudgetExceededResponse();
            }
            throw e;
        }
    }

    private FollowUpResponse processUserTurnInternal(
            Long interviewId, int durationMinutes,
            String questionContent, String answerText,
            List<FollowUpExchange> previousExchanges,
            ResumeSkeleton skeleton, InterviewPlan plan,
            boolean terminate
    ) {
        clockWatcher.markStart(interviewId);

        long turnIndex = previousExchanges != null ? previousExchanges.size() : 0;
        TurnAnalysisResult turnResult = turnAnalysisPipeline.analyze(
                interviewId, turnIndex, questionContent, answerText, previousExchanges);
        AnswerAnalysis analysis = turnResult.answerAnalysis();

        long remainingMinutes = clockWatcher.remainingMinutes(interviewId, durationMinutes);

        if (modeTransitionPolicy.isHardTimeoutExceeded(durationMinutes, remainingMinutes)) {
            log.warn("[ResumeOrchestrator] hard timeout backstop: interviewId={}", interviewId);
            return hardTimeoutResponse();
        }

        if (terminate) {
            log.info("[ResumeOrchestrator] FE-signaled terminate: interviewId={}, lastQuestionAnalyzed=true", interviewId);
            return terminateResponse();
        }

        InterviewRuntimeState currentState = runtimeStateStore.get(interviewId);
        ResumeMode currentMode = currentState.getResumeMode();
        ChainStateTracker chainTracker = currentState.getChainStateTracker();
        int currentChainLevel = chainTracker != null ? chainTracker.getCurrentLevel() : 1;

        TurnHandlerResult handlerResult = dispatchByMode(
                currentMode, interviewId, currentState, answerText, analysis,
                skeleton, plan, previousExchanges);

        if (shouldSkipTurnCompletedEvent(handlerResult)) {
            log.warn("[진행차단진단] interviewId={} track=RESUME stage={} reason=publish-skip turnIndex={}",
                    interviewId, currentMode.name().toLowerCase(), turnIndex);
            return handlerResult.response();
        }
        validateQuestionId(interviewId, turnIndex, currentMode, handlerResult);
        validateResponseQuestionId(interviewId, turnIndex, currentMode, handlerResult);

        turnEventPublisher.publish(interviewId, turnIndex, analysis, currentMode,
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
                    .questionId(opener.getId())
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
            ResumeSkeleton skeleton, InterviewPlan plan,
            List<FollowUpExchange> previousExchanges
    ) {
        return switch (mode) {
            case PLAYGROUND -> handlePlayground(interviewId, state, answerText, analysis, skeleton, plan, previousExchanges);
            case INTERROGATION -> {
                InterrogationTurnResult r =
                        interrogationHandler.handle(interviewId, state, answerText, analysis, plan, previousExchanges);
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
            InterrogationTurnResult interrogationResult =
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

    // type 미설정 의도 — tech-spec 424 Option C (응답 schema 구분 회피) 채택.
    private FollowUpResponse terminateResponse() {
        return FollowUpResponse.builder()
                .followUpExhausted(true)
                .skip(true)
                .presentToUser(false)
                .build();
    }

    private FollowUpResponse contextBudgetExceededResponse() {
        return FollowUpResponse.builder()
                .followUpExhausted(true)
                .skip(true)
                .presentToUser(false)
                .type("CONTEXT_BUDGET_EXCEEDED")
                .build();
    }

    private boolean shouldSkipTurnCompletedEvent(TurnHandlerResult result) {
        FollowUpResponse response = result.response();
        return result.questionId() == null
                && response.isSkip()
                && !response.isPresentToUser();
    }

    private void validateQuestionId(Long interviewId, long turnIndex, ResumeMode mode, TurnHandlerResult result) {
        if (result.questionId() != null) {
            return;
        }
        log.warn("[진행차단진단] interviewId={} track=RESUME stage={} reason=questionId-missing turnIndex={} type={}",
                interviewId, mode.name().toLowerCase(), turnIndex, result.response().getType());
        throw new BusinessException(ResumeErrorCode.QUESTION_ID_MISSING);
    }

    private void validateResponseQuestionId(Long interviewId, long turnIndex, ResumeMode mode, TurnHandlerResult result) {
        Long handlerId = result.questionId();
        Long responseId = result.response().getQuestionId();
        if (responseId != null && responseId.equals(handlerId)) {
            return;
        }
        log.warn("[진행차단진단] interviewId={} track=RESUME stage={} reason=response-questionid-mismatch handlerQuestionId={} responseQuestionId={} turnIndex={}",
                interviewId, mode.name().toLowerCase(), handlerId, responseId, turnIndex);
    }

    private record TurnHandlerResult(FollowUpResponse response, Long questionId) {}
}
