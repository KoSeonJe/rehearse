package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaygroundModeHandler {

    private final ResumeQuestionResultGenerator resultGenerator;
    private final ResumeQuestionPersister questionPersister;

    public OpenerResult handleOpener(
            Long interviewId, InterviewRuntimeState state,
            ResumeSkeleton skeleton, InterviewPlan plan
    ) {
        ProjectPlan firstPlan = plan.projectPlans().get(0);
        Project project = findProject(skeleton, firstPlan.projectId());

        PlaygroundOpenerResult result = resultGenerator.generateOpener(interviewId, state, project, firstPlan.playgroundPhase());

        int orderIndex = state.nextResumeOrderIndex();
        Long questionId = questionPersister.persist(
                interviewId, QuestionType.RESUME_OPENER, result.question(),
                result.ttsQuestion(), result.modelAnswer(), orderIndex);

        log.info("[PlaygroundHandler] 오프너 생성: interviewId={}, projectId={}, questionId={}",
                interviewId, firstPlan.projectId(), questionId);
        state.getPlaygroundTurns().incrementAndGet();

        FollowUpResponse response = buildResponse(result.question(), result.ttsQuestion(), result.reason(), false);
        return new OpenerResult(response, questionId);
    }

    public PlaygroundTurnResult handle(
            Long interviewId, InterviewRuntimeState state,
            String userAnswer, AnswerAnalysis analysis,
            ResumeSkeleton skeleton, InterviewPlan plan,
            List<FollowUpExchange> previousExchanges
    ) {
        ProjectPlan currentPlan = resolveCurrentPlan(plan);
        PlaygroundPhase phase = currentPlan.playgroundPhase();
        List<String> expectedClaims = phase.expectedClaimsCoverage();
        Project project = findProject(skeleton, currentPlan.projectId());

        int turnCount = state.getPlaygroundTurns().get();
        int cumulativeLength = accumulateLength(state, userAnswer);

        PlaygroundResponderResult result = resultGenerator.generatePlaygroundResponder(
                interviewId, state, previousExchanges,
                project, userAnswer, expectedClaims, turnCount, cumulativeLength
        );

        boolean shouldSwitch = evaluateSwitchConditions(result, turnCount + 1);
        boolean questionBlank = result.question() == null || result.question().isBlank();

        if (questionBlank && !shouldSwitch) {
            throw new BusinessException(AiErrorCode.RESPONSE_INVALID);
        }

        state.getPlaygroundTurns().incrementAndGet();

        Long questionId = null;
        if (!questionBlank) {
            int orderIndex = state.nextResumeOrderIndex();
            questionId = questionPersister.persist(
                    interviewId, QuestionType.RESUME_PLAYGROUND, result.question(),
                    result.ttsQuestion(), result.modelAnswer(), orderIndex);
        }

        if (shouldSwitch) {
            log.info("[PlaygroundHandler] Interrogation 전환 결정: interviewId={}, turnCount={}", interviewId, turnCount + 1);
        }

        FollowUpResponse response = buildResponse(result.question(), result.ttsQuestion(), result.reason(), shouldSwitch);
        return new PlaygroundTurnResult(response, shouldSwitch, questionId);
    }

    private boolean evaluateSwitchConditions(PlaygroundResponderResult result, int turnCount) {
        if (result.shouldSwitchToInterrogation()) {
            return true;
        }
        if (result.switchConditionsMet() == null) {
            return false;
        }
        PlaygroundResponderResult.SwitchConditions cond = result.switchConditionsMet();
        int metCount = 0;
        if (cond.aCovered()) metCount++;
        if (cond.bLengthOk()) metCount++;
        if (cond.cSignal()) metCount++;
        if (cond.dTurnLimit()) metCount++;
        return metCount >= 2;
    }

    private int accumulateLength(InterviewRuntimeState state, String currentAnswer) {
        int length = currentAnswer != null ? currentAnswer.length() : 0;
        return state.addPlaygroundAnswerLength(length);
    }

    private ProjectPlan resolveCurrentPlan(InterviewPlan plan) {
        return plan.projectPlans().get(0);
    }

    private Project findProject(ResumeSkeleton skeleton, String projectId) {
        return skeleton.projects().stream()
                .filter(p -> projectId.equals(p.projectId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResumeErrorCode.PROJECT_NOT_FOUND_IN_SKELETON));
    }

    private FollowUpResponse buildResponse(String question, String ttsQuestion, String reason, boolean transitioned) {
        return FollowUpResponse.builder()
                .question(question)
                .ttsQuestion(ttsQuestion)
                .reason(reason)
                .type("RESUME_PLAYGROUND")
                .skip(false)
                .presentToUser(true)
                .followUpExhausted(transitioned)
                .build();
    }

    public record OpenerResult(FollowUpResponse response, Long questionId) {}

    public record PlaygroundTurnResult(FollowUpResponse response, boolean switchedToInterrogation, Long questionId) {}
}
