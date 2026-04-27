package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterrogationModeHandler {

    private final ResumeChainInterrogatorPromptBuilder promptBuilder;

    public FollowUpResponse handle(
            Long interviewId, InterviewRuntimeState state,
            String userAnswer, AnswerAnalysis analysis,
            InterviewPlan plan
    ) {
        ChainStateTracker tracker = state.getChainStateTracker();

        return tracker.withLock(() -> {
            if (!tracker.hasActiveChain()) {
                Optional<ChainReference> nextChain = tracker.resolveNextChain(plan.projectPlans());
                if (nextChain.isEmpty()) {
                    log.info("[InterrogationHandler] 모든 chain 소진: interviewId={}", interviewId);
                    return buildExhaustedResponse();
                }
                ChainReference chain = nextChain.get();
                tracker.initChain(chain.projectId(), chain.chainId());
                log.info("[InterrogationHandler] 새 chain 시작: interviewId={}, chainId={}", interviewId, chain.chainId());
            }

            int answerQuality = analysis != null ? analysis.answerQuality() : 2;
            int currentLevel = tracker.getCurrentLevel();
            int consecutiveStay = tracker.getConsecutiveLevelStayCount();
            String chainTopic = tracker.getCurrentChainId();

            InterrogationResult result = promptBuilder.build(
                    chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStay
            );

            applyDecision(tracker, result, answerQuality, currentLevel);

            log.info("[InterrogationHandler] turn 처리: interviewId={}, chainId={}, level={}, action={}",
                    interviewId, chainTopic, currentLevel, result.nextAction());

            return buildResponse(result, tracker.getCurrentLevel());
        });
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
