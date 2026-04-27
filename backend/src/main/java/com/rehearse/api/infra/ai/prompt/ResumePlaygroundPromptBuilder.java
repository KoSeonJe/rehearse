package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumePlaygroundPromptBuilder {

    private static final String OPENER_CALL_TYPE = "resume_playground_opener";
    private static final String RESPONDER_CALL_TYPE = "resume_playground_responder";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;

    @Value("${rehearse.resume-track.model:gpt-4o-mini}")
    private String model;

    @Value("${rehearse.resume-track.temperature:0.7}")
    private double temperature;

    @Value("${rehearse.resume-track.max-tokens:800}")
    private int maxTokens;

    public PlaygroundOpenerResult buildOpener(Long interviewId, Project project, PlaygroundPhase phase) {
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                OPENER_CALL_TYPE,
                Map.of(),
                List.of(),
                Map.of(
                        "PROJECT_INFO", formatProjectInfo(project),
                        "OPENER_QUESTION", phase.openerQuestion()
                ),
                null
        ));

        ChatRequest request = ChatRequest.builder()
                .messages(built.messages())
                .callType(OPENER_CALL_TYPE)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(request);
        return aiResponseParser.parseOrRetry(response, PlaygroundOpenerResult.class, aiClient, request);
    }

    public PlaygroundResponderResult buildResponder(
            Long interviewId, String userAnswer, List<String> expectedClaims,
            int playgroundTurnCount, int cumulativeLength
    ) {
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                RESPONDER_CALL_TYPE,
                Map.of(),
                List.of(),
                Map.of(
                        "EXPECTED_CLAIMS", String.join("\n", expectedClaims),
                        "PLAYGROUND_TURN_COUNT", String.valueOf(playgroundTurnCount),
                        "CUMULATIVE_UTTERANCE_LENGTH", String.valueOf(cumulativeLength),
                        "USER_ANSWER", userAnswer != null ? userAnswer : ""
                ),
                null
        ));

        ChatRequest request = ChatRequest.builder()
                .messages(built.messages())
                .callType(RESPONDER_CALL_TYPE)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(request);
        return aiResponseParser.parseOrRetry(response, PlaygroundResponderResult.class, aiClient, request);
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
