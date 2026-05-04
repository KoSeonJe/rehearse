package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ResumePlaygroundPromptBuilder extends AbstractResumeJsonPromptBuilder {

    private static final String OPENER_CALL_TYPE = "resume_playground_opener";
    private static final String RESPONDER_CALL_TYPE = "resume_playground_responder";

    public ResumePlaygroundPromptBuilder(
            AiClient aiClient,
            AiResponseParser aiResponseParser,
            InterviewContextBuilder contextBuilder,
            @Value("${rehearse.resume-track.model:gpt-4o-mini}") String model,
            @Value("${rehearse.resume-track.temperature:0.7}") double temperature,
            @Value("${rehearse.resume-track.max-tokens:800}") int maxTokens
    ) {
        super(aiClient, aiResponseParser, contextBuilder, model, temperature, maxTokens);
    }

    public PlaygroundOpenerResult buildOpener(
            Long interviewId, InterviewRuntimeState state,
            Project project, PlaygroundPhase phase
    ) {
        return executeJson(
                OPENER_CALL_TYPE, interviewId, state, List.of(),
                new FocusHints.ResumePlaygroundOpenerHints(
                        formatProjectInfo(project),
                        phase.openerQuestion()
                ),
                PlaygroundOpenerResult.class
        );
    }

    public PlaygroundResponderResult buildResponder(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> exchanges,
            String userAnswer, List<String> expectedClaims,
            int playgroundTurnCount, int cumulativeLength
    ) {
        return executeJson(
                RESPONDER_CALL_TYPE, interviewId, state, exchanges,
                new FocusHints.ResumePlaygroundResponderHints(
                        String.join("\n", expectedClaims),
                        userAnswer != null ? userAnswer : "",
                        playgroundTurnCount,
                        cumulativeLength
                ),
                PlaygroundResponderResult.class
        );
    }

    private static String formatProjectInfo(Project project) {
        return "projectId: " + project.projectId()
                + "\nclaims: " + project.claims().size() + "개"
                + "\nimplicitCsTopics: " + project.implicitCsTopics().size() + "개";
    }

    public record PlaygroundOpenerResult(
            String question,
            @JsonProperty("tts_question") String ttsQuestion,
            String reason
    ) {}

    public record PlaygroundResponderResult(
            String question,
            @JsonProperty("tts_question") String ttsQuestion,
            String reason,
            @JsonProperty("should_switch_to_interrogation") boolean shouldSwitchToInterrogation,
            @JsonProperty("switch_conditions_met") SwitchConditions switchConditionsMet
    ) {
        public record SwitchConditions(
                @JsonProperty("a_covered") boolean aCovered,
                @JsonProperty("b_length_ok") boolean bLengthOk,
                @JsonProperty("c_signal") boolean cSignal,
                @JsonProperty("d_turn_limit") boolean dTurnLimit
        ) {}
    }
}
