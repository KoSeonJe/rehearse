package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.entity.ChainStateTracker;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.infra.ai.prompt.ResumeWrapUpPromptBuilder.WrapUpResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WrapUpModeHandler {

    private final ResumeQuestionResultGenerator resultGenerator;
    private final ResumeQuestionPersister questionPersister;

    public WrapUpTurnResult handle(
            Long interviewId, InterviewRuntimeState state,
            String userAnswer, AnswerAnalysis analysis,
            InterviewPlan plan,
            long remainingMinutes, boolean isRetrospective,
            List<FollowUpExchange> previousExchanges
    ) {
        String sessionSummary = buildSessionSummary(state);

        WrapUpResult result = resultGenerator.generateWrapUp(
                interviewId, state, previousExchanges,
                sessionSummary, remainingMinutes, isRetrospective);

        log.info("[WrapUpHandler] 회고 질문 생성: interviewId={}, remainingMin={}, isRetrospective={}",
                interviewId, remainingMinutes, isRetrospective);

        int orderIndex = state.nextResumeOrderIndex();
        Long questionId = questionPersister.persist(
                interviewId, QuestionType.RESUME_WRAP_UP, result.question(),
                result.ttsQuestion(), result.modelAnswer(), orderIndex);

        boolean exhausted = result.sessionComplete() || remainingMinutes <= 0;

        FollowUpResponse response = FollowUpResponse.builder()
                .question(result.question())
                .ttsQuestion(result.ttsQuestion())
                .reason(result.reason())
                .type("RESUME_WRAP_UP")
                .skip(false)
                .presentToUser(true)
                .followUpExhausted(exhausted)
                .build();
        return new WrapUpTurnResult(response, questionId);
    }

    public record WrapUpTurnResult(FollowUpResponse response, Long questionId) {}

    private String buildSessionSummary(InterviewRuntimeState state) {
        ChainStateTracker tracker = state.getChainStateTracker();
        return "완료된 chain: " + tracker.getCompletedChainIds().size()
                + ", 현재 chain: " + (tracker.getCurrentChainId() != null ? tracker.getCurrentChainId() : "없음")
                + ", 현재 레벨: " + tracker.getCurrentLevel()
                + ", Playground 턴: " + state.getPlaygroundTurns().get();
    }
}
