package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.ChainStateTracker;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterrogationModeHandler {

    private final ResumeChainInterrogatorPromptBuilder promptBuilder;
    private final ResumeQuestionPersister questionPersister;

    public InterrogationTurnResult handle(
            Long interviewId, InterviewRuntimeState state,
            String userAnswer, AnswerAnalysis analysis,
            InterviewPlan plan,
            List<FollowUpExchange> previousExchanges
    ) {
        ChainStateTracker tracker = state.getChainStateTracker();

        // Phase 1: chain state 진입 + 스냅샷 캡처 (lock 안)
        Optional<ChainStateTrackerSnapshot> snapshotOpt = tracker.withLock(() ->
                acquireSnapshot(interviewId, tracker, state, analysis, plan));
        if (snapshotOpt.isEmpty()) {
            log.info("[InterrogationHandler] 모든 chain 소진: interviewId={}", interviewId);
            return new InterrogationTurnResult(buildExhaustedResponse(), null);
        }
        ChainStateTrackerSnapshot snapshot = snapshotOpt.get();

        // Phase 2: LLM 호출 + 응답 검증 (lock 밖)
        InterrogationResult result = promptBuilder.build(
                interviewId, state, previousExchanges,
                snapshot.chainTopic(), snapshot.currentLevel(),
                snapshot.answerQuality(), userAnswer, snapshot.consecutiveStay()
        );

        if (result.question() == null || result.question().isBlank()) {
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }
        if (ResumeFallbackQuestions.INTERROGATION.equals(result.question())) {
            log.warn("[InterrogationHandler] 안전 폴백 사용 감지: interviewId={}, chainId={}, level={}",
                    interviewId, snapshot.chainTopic(), snapshot.currentLevel());
        }

        // Phase 3: DB persist + tracker 상태 변경 (lock 안, 원자 묶음)
        return tracker.withLock(() -> {
            Long questionId = questionPersister.persist(
                    interviewId, QuestionType.RESUME_INTERROGATION, result.question(), snapshot.orderIndex());
            applyDecision(tracker, result, snapshot.answerQuality(), snapshot.currentLevel());
            log.info("[InterrogationHandler] turn 처리: interviewId={}, chainId={}, level={}, action={}",
                    interviewId, snapshot.chainTopic(), snapshot.currentLevel(), result.nextAction());
            return new InterrogationTurnResult(buildResponse(result, tracker.getCurrentLevel()), questionId);
        });
    }

    private Optional<ChainStateTrackerSnapshot> acquireSnapshot(
            Long interviewId, ChainStateTracker tracker, InterviewRuntimeState state,
            AnswerAnalysis analysis, InterviewPlan plan
    ) {
        if (!tracker.hasActiveChain()) {
            Optional<ChainReference> nextChain = tracker.resolveNextChain(plan.projectPlans());
            if (nextChain.isEmpty()) {
                return Optional.empty();
            }
            ChainReference chain = nextChain.get();
            tracker.initChain(chain.projectId(), chain.chainId());
            log.info("[InterrogationHandler] 새 chain 시작: interviewId={}, chainId={}", interviewId, chain.chainId());
        }
        int answerQuality = analysis != null ? analysis.answerQuality() : 2;
        int orderIndex = state.nextResumeOrderIndex();
        return Optional.of(new ChainStateTrackerSnapshot(
                tracker.getCurrentChainId(),
                tracker.getCurrentLevel(),
                tracker.getConsecutiveLevelStayCount(),
                tracker.getCurrentProjectId(),
                orderIndex,
                answerQuality
        ));
    }

    private void applyDecision(ChainStateTracker tracker, InterrogationResult result, int answerQuality, int currentLevel) {
        if (result.isLevelUp()) {
            tracker.levelUp();
            return;
        }
        if (result.isChainSwitch()) {
            tracker.markChainComplete();
            return;
        }
        if (!result.isLevelStay()) {
            log.warn("[InterrogationHandler] 알 수 없는 next_action: '{}' → LEVEL_STAY 처리", result.nextAction());
        }
        boolean stayLimitExceeded = tracker.levelStay();
        if (stayLimitExceeded) {
            if (currentLevel < 4) {
                tracker.levelUp();
                log.info("[InterrogationHandler] LEVEL_STAY 한계 초과 → 강제 LEVEL_UP: level={}", currentLevel + 1);
            } else {
                tracker.markChainComplete();
                log.info("[InterrogationHandler] LEVEL_STAY 한계 초과 + level=4 → CHAIN_SWITCH 강제");
            }
        }
    }

    private FollowUpResponse buildResponse(InterrogationResult result, int currentLevel) {
        return FollowUpResponse.builder()
                .question(result.question())
                .ttsQuestion(result.ttsQuestion())
                .reason(result.reason())
                .type("RESUME_INTERROGATION_L" + currentLevel)
                .skip(false)
                .presentToUser(true)
                .followUpExhausted(false)
                .build();
    }

    private FollowUpResponse buildExhaustedResponse() {
        return FollowUpResponse.builder()
                .question(null)
                .skip(true)
                .presentToUser(false)
                .followUpExhausted(true)
                .type("RESUME_INTERROGATION_EXHAUSTED")
                .build();
    }
}
