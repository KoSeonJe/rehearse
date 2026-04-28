package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.service.AnswerAnalyzer;
import com.rehearse.api.domain.interview.service.IntentClassifier;
import com.rehearse.api.domain.interview.service.IntentDispatcher;
import com.rehearse.api.domain.interview.service.IntentBranchInput;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.ResumeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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

    private final IntentClassifier intentClassifier;
    private final AnswerAnalyzer answerAnalyzer;
    private final IntentDispatcher intentDispatcher;
    private final PlaygroundModeHandler playgroundHandler;
    private final InterrogationModeHandler interrogationHandler;
    private final WrapUpModeHandler wrapUpHandler;
    private final ClockWatcher clockWatcher;
    private final InterviewRuntimeStateStore runtimeStateStore;
    private final InterviewFinder interviewFinder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${rehearse.resume-track.wrap-up-threshold-min:2}")
    private long wrapUpThresholdMin;

    @Value("${rehearse.resume-track.hard-timeout-min:10}")
    private long hardTimeoutMin;

    public FollowUpResponse processUserTurn(
            Long interviewId, int durationMinutes,
            String questionContent, String answerText,
            List<FollowUpExchange> previousExchanges,
            ResumeSkeleton skeleton, InterviewPlan plan
    ) {
        clockWatcher.markStart(interviewId);

        IntentResult intent = intentClassifier.classify(questionContent, answerText, previousExchanges);
        if (intent.type() != IntentType.ANSWER) {
            log.info("[ResumeOrchestrator] non-answer intent: interviewId={}, intent={}", interviewId, intent.type());
            return handleNonAnswerIntent(interviewId, questionContent, answerText, intent, previousExchanges);
        }

        AnswerAnalysis analysis = answerAnalyzer.analyze(
                interviewId,
                (long) (previousExchanges != null ? previousExchanges.size() : 0),
                questionContent,
                null,
                answerText,
                List.of()
        );

        InterviewRuntimeState state = runtimeStateStore.get(interviewId);

        long remainingMinutes = clockWatcher.remainingMinutes(interviewId, durationMinutes);
        if (remainingMinutes <= wrapUpThresholdMin && state.getResumeMode() != ResumeMode.WRAP_UP) {
            log.info("[ResumeOrchestrator] WRAP_UP 전이: interviewId={}, remainingMin={}", interviewId, remainingMinutes);
            runtimeStateStore.update(interviewId, s -> s.transitionTo(ResumeMode.WRAP_UP));
        }

        ResumeMode currentMode = runtimeStateStore.get(interviewId).getResumeMode();

        if (currentMode == ResumeMode.WRAP_UP) {
            long elapsedMinutes = durationMinutes - remainingMinutes;
            if (elapsedMinutes >= durationMinutes + hardTimeoutMin) {
                log.warn("[ResumeOrchestrator] hard timeout 초과 → 강제 종료: interviewId={}, elapsedMin={}", interviewId, elapsedMinutes);
                return FollowUpResponse.builder()
                        .followUpExhausted(true)
                        .skip(true)
                        .presentToUser(false)
                        .type("RESUME_HARD_TIMEOUT")
                        .build();
            }
        }

        long turnIndex = previousExchanges != null ? previousExchanges.size() : 0;
        ChainStateTracker chainTracker = state.getChainStateTracker();
        int currentChainLevel = chainTracker != null ? chainTracker.getCurrentLevel() : 1;

        FollowUpResponse response = switch (currentMode) {
            case PLAYGROUND -> handlePlayground(interviewId, state, answerText, analysis, skeleton, plan);
            case INTERROGATION -> interrogationHandler.handle(interviewId, state, answerText, analysis, plan);
            case WRAP_UP -> wrapUpHandler.handle(interviewId, state, answerText, analysis, remainingMinutes, true);
        };

        publishResumeTurnCompletedEvent(interviewId, turnIndex, analysis, intent, currentMode, currentChainLevel, skeleton, answerText);

        return response;
    }

    public FollowUpResponse startSession(
            Long interviewId, int durationMinutes,
            ResumeSkeleton skeleton, InterviewPlan plan
    ) {
        clockWatcher.markStart(interviewId);
        InterviewRuntimeState state = runtimeStateStore.get(interviewId);
        log.info("[ResumeOrchestrator] 세션 시작: interviewId={}, mode=PLAYGROUND", interviewId);
        return playgroundHandler.handleOpener(interviewId, state, skeleton, plan);
    }

    private FollowUpResponse handlePlayground(
            Long interviewId, InterviewRuntimeState state,
            String answerText, AnswerAnalysis analysis,
            ResumeSkeleton skeleton, InterviewPlan plan
    ) {
        PlaygroundModeHandler.PlaygroundTurnResult result =
                playgroundHandler.handle(interviewId, state, answerText, analysis, skeleton, plan);

        if (result.switchedToInterrogation()) {
            runtimeStateStore.update(interviewId, s -> s.transitionTo(ResumeMode.INTERROGATION));
            InterviewRuntimeState refreshed = runtimeStateStore.get(interviewId);
            return interrogationHandler.handle(interviewId, refreshed, null, null, plan);
        }
        return result.response();
    }

    private void publishResumeTurnCompletedEvent(
            Long interviewId, long turnIndex, AnswerAnalysis analysis,
            IntentResult intent, ResumeMode currentMode, int currentChainLevel,
            ResumeSkeleton skeleton, String userAnswer
    ) {
        try {
            Interview interview = interviewFinder.findById(interviewId);
            TurnCompletedEvent event = TurnCompletedEvent.ofResumeTrack(
                    interviewId, turnIndex, interview.getUserId(),
                    null, null,
                    userAnswer != null ? userAnswer : "",
                    analysis, intent.type(), interview.getLevel(),
                    currentMode, currentChainLevel, skeleton
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Resume TurnCompletedEvent 발행 실패 — 턴 진행 차단하지 않음: interviewId={}, reason={}",
                    interviewId, e.getMessage());
        }
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
}
