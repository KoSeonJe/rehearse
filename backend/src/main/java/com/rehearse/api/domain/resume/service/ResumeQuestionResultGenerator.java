package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder.WrapUpResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeQuestionResultGenerator {

    private static final String MODE_OPENER = "OPENER";
    private static final String MODE_PLAYGROUND = "PLAYGROUND";
    private static final String MODE_INTERROGATION = "INTERROGATION";
    private static final String MODE_WRAP_UP = "WRAP_UP";

    private final ResumePlaygroundPromptBuilder playgroundPromptBuilder;
    private final ResumeChainInterrogatorPromptBuilder chainInterrogatorPromptBuilder;
    private final ResumeWrapUpPromptBuilder wrapUpPromptBuilder;

    public PlaygroundOpenerResult generateOpener(
            Long interviewId, InterviewRuntimeState state,
            Project project, PlaygroundPhase phase
    ) {
        PlaygroundOpenerResult first = playgroundPromptBuilder.buildOpener(interviewId, state, project, phase);
        if (!isBlank(first.modelAnswer())) {
            return first;
        }
        PlaygroundOpenerResult second = playgroundPromptBuilder.buildOpener(interviewId, state, project, phase);
        if (!isBlank(second.modelAnswer())) {
            return second;
        }
        warnFallback(interviewId, MODE_OPENER);
        return second.withModelAnswer(ResumeFallbackModelAnswers.OPENER);
    }

    public PlaygroundResponderResult generatePlaygroundResponder(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> previousExchanges,
            Project project, String userAnswer, List<String> expectedClaims,
            int playgroundTurnCount, int cumulativeLength
    ) {
        PlaygroundResponderResult first = playgroundPromptBuilder.buildResponder(
                interviewId, state, previousExchanges, project, userAnswer,
                expectedClaims, playgroundTurnCount, cumulativeLength);
        if (!isBlank(first.modelAnswer())) {
            return first;
        }
        PlaygroundResponderResult second = playgroundPromptBuilder.buildResponder(
                interviewId, state, previousExchanges, project, userAnswer,
                expectedClaims, playgroundTurnCount, cumulativeLength);
        if (!isBlank(second.modelAnswer())) {
            return second;
        }
        warnFallback(interviewId, MODE_PLAYGROUND);
        return second.withModelAnswer(ResumeFallbackModelAnswers.PLAYGROUND);
    }

    public InterrogationResult generateInterrogation(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> previousExchanges,
            String projectName, String chainTopic, int currentLevel, int answerQuality,
            String userAnswer, int consecutiveStayCount
    ) {
        InterrogationResult first = chainInterrogatorPromptBuilder.build(
                interviewId, state, previousExchanges, projectName,
                chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStayCount);
        if (!isBlank(first.modelAnswer())) {
            return first;
        }
        InterrogationResult second = chainInterrogatorPromptBuilder.build(
                interviewId, state, previousExchanges, projectName,
                chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStayCount);
        if (!isBlank(second.modelAnswer())) {
            return second;
        }
        warnFallback(interviewId, MODE_INTERROGATION);
        return second.withModelAnswer(ResumeFallbackModelAnswers.INTERROGATION);
    }

    public WrapUpResult generateWrapUp(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> previousExchanges,
            String sessionSummary, long remainingMinutes, boolean isRetrospective
    ) {
        WrapUpResult first = wrapUpPromptBuilder.build(
                interviewId, state, previousExchanges, sessionSummary, remainingMinutes, isRetrospective);
        if (!isBlank(first.modelAnswer())) {
            return first;
        }
        WrapUpResult second = wrapUpPromptBuilder.build(
                interviewId, state, previousExchanges, sessionSummary, remainingMinutes, isRetrospective);
        if (!isBlank(second.modelAnswer())) {
            return second;
        }
        warnFallback(interviewId, MODE_WRAP_UP);
        return second.withModelAnswer(ResumeFallbackModelAnswers.WRAP_UP);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void warnFallback(Long interviewId, String mode) {
        log.warn("[ResumeQuestionResultGenerator] modelAnswer 폴백 적용: interviewId={}, mode={}",
                interviewId, mode);
    }
}
