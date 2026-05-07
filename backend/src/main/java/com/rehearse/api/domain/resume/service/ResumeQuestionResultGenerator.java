package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
        PlaygroundOpenerResult result = first;
        if (needsRetry(first.modelAnswer(), first.question())) {
            result = playgroundPromptBuilder.buildOpener(interviewId, state, project, phase);
        }
        if (isBlank(result.question())) {
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }
        warnIfQuestionFallback(interviewId, MODE_OPENER, result.question(), ResumeFallbackQuestions.OPENER);
        if (isBlank(result.modelAnswer())) {
            warnModelAnswerFallback(interviewId, MODE_OPENER);
            return result.withModelAnswer(ResumeFallbackModelAnswers.OPENER);
        }
        return result;
    }

    public PlaygroundResponderResult generatePlaygroundResponder(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> previousExchanges,
            Project project, String userAnswer, List<String> expectedClaims,
            int playgroundTurnCount, int cumulativeLength
    ) {
        PlaygroundResponderResult first = playgroundPromptBuilder.buildResponder(
                interviewId, state, previousExchanges, project, userAnswer,
                expectedClaims, playgroundTurnCount, cumulativeLength);
        PlaygroundResponderResult result = first;
        if (needsRetry(first.modelAnswer(), first.question())) {
            result = playgroundPromptBuilder.buildResponder(
                    interviewId, state, previousExchanges, project, userAnswer,
                    expectedClaims, playgroundTurnCount, cumulativeLength);
        }
        warnIfQuestionFallback(interviewId, MODE_PLAYGROUND, result.question(), ResumeFallbackQuestions.PLAYGROUND_RESPONDER);
        if (isBlank(result.modelAnswer())) {
            warnModelAnswerFallback(interviewId, MODE_PLAYGROUND);
            return result.withModelAnswer(ResumeFallbackModelAnswers.PLAYGROUND);
        }
        return result;
    }

    public InterrogationResult generateInterrogation(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> previousExchanges,
            String projectName, String chainTopic, int currentLevel, int answerQuality,
            String userAnswer, int consecutiveStayCount
    ) {
        InterrogationResult first = chainInterrogatorPromptBuilder.build(
                interviewId, state, previousExchanges, projectName,
                chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStayCount);
        InterrogationResult result = first;
        if (needsRetry(first.modelAnswer(), first.question())) {
            result = chainInterrogatorPromptBuilder.build(
                    interviewId, state, previousExchanges, projectName,
                    chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStayCount);
        }
        if (isBlank(result.question())) {
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }
        warnIfQuestionFallback(interviewId, MODE_INTERROGATION, result.question(), ResumeFallbackQuestions.INTERROGATION);
        if (isBlank(result.modelAnswer())) {
            warnModelAnswerFallback(interviewId, MODE_INTERROGATION);
            return result.withModelAnswer(ResumeFallbackModelAnswers.INTERROGATION);
        }
        return result;
    }

    public WrapUpResult generateWrapUp(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> previousExchanges,
            String sessionSummary, long remainingMinutes, boolean isRetrospective
    ) {
        WrapUpResult first = wrapUpPromptBuilder.build(
                interviewId, state, previousExchanges, sessionSummary, remainingMinutes, isRetrospective);
        WrapUpResult result = first;
        if (needsRetry(first.modelAnswer(), first.question())) {
            result = wrapUpPromptBuilder.build(
                    interviewId, state, previousExchanges, sessionSummary, remainingMinutes, isRetrospective);
        }
        if (isBlank(result.question())) {
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }
        warnIfQuestionFallback(interviewId, MODE_WRAP_UP, result.question(), ResumeFallbackQuestions.WRAP_UP);
        if (isBlank(result.modelAnswer())) {
            warnModelAnswerFallback(interviewId, MODE_WRAP_UP);
            return result.withModelAnswer(ResumeFallbackModelAnswers.WRAP_UP);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean needsRetry(String modelAnswer, String question) {
        return isBlank(modelAnswer) || isBlank(question);
    }

    private static void warnModelAnswerFallback(Long interviewId, String mode) {
        log.warn("[ResumeQuestionResultGenerator] modelAnswer 폴백 적용: interviewId={}, mode={}",
                interviewId, mode);
    }

    private static void warnIfQuestionFallback(Long interviewId, String mode, String question, String fallback) {
        if (fallback.equals(question)) {
            log.warn("[ResumeQuestionResultGenerator] question 폴백 텍스트 일치 감지: interviewId={}, mode={}",
                    interviewId, mode);
        }
    }
}
