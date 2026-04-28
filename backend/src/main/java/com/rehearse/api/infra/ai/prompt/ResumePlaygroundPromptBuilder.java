package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    public PlaygroundOpenerResult buildOpener(Long interviewId, Project project, PlaygroundPhase phase) {
        return executeJson(OPENER_CALL_TYPE, Map.of(
                "PROJECT_INFO", formatProjectInfo(project),
                "OPENER_QUESTION", phase.openerQuestion()
        ), PlaygroundOpenerResult.class);
    }

    public PlaygroundResponderResult buildResponder(
            Long interviewId, String userAnswer, List<String> expectedClaims,
            int playgroundTurnCount, int cumulativeLength
    ) {
        return executeJson(RESPONDER_CALL_TYPE, Map.of(
                "EXPECTED_CLAIMS", String.join("\n", expectedClaims),
                "PLAYGROUND_TURN_COUNT", String.valueOf(playgroundTurnCount),
                "CUMULATIVE_UTTERANCE_LENGTH", String.valueOf(cumulativeLength),
                "USER_ANSWER", userAnswer != null ? userAnswer : ""
        ), PlaygroundResponderResult.class);
    }

    private static String formatProjectInfo(Project project) {
        return "projectId: " + project.projectId()
                + "\nclaims: " + project.claims().size() + "개"
                + "\nimplicitCsTopics: " + project.implicitCsTopics().size() + "개";
    }

    public record PlaygroundOpenerResult(String question, String ttsQuestion, String reason) {}

    public record PlaygroundResponderResult(
            String question, String ttsQuestion, String reason,
            boolean shouldSwitchToInterrogation,
            SwitchConditions switchConditionsMet
    ) {
        public record SwitchConditions(
                boolean aCovered, boolean bLengthOk, boolean cSignal, boolean dTurnLimit
        ) {}
    }
}
