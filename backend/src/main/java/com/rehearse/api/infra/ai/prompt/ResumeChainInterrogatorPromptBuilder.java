package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ResumeChainInterrogatorPromptBuilder {

    private static final String CALL_TYPE = "resume_chain_interrogator";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;

    @Value("${rehearse.resume-track.model:gpt-4o-mini}")
    private String model;

    @Value("${rehearse.resume-track.temperature:0.7}")
    private double temperature;

    @Value("${rehearse.resume-track.max-tokens:800}")
    private int maxTokens;

    public InterrogationResult build(
            String chainTopic, int currentLevel, int answerQuality,
            String userAnswer, int consecutiveStayCount
    ) {
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                Map.of(),
                List.of(),
                Map.of(
                        "CURRENT_CHAIN", chainTopic,
                        "CURRENT_LEVEL", String.valueOf(currentLevel),
                        "ANSWER_QUALITY", String.valueOf(answerQuality),
                        "USER_ANSWER", userAnswer != null ? userAnswer : "",
                        "CONSECUTIVE_STAY_COUNT", String.valueOf(consecutiveStayCount)
                ),
                null
        ));

        ChatRequest request = ChatRequest.builder()
                .messages(built.messages())
                .callType(CALL_TYPE)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(request);
        return aiResponseParser.parseOrRetry(response, InterrogationResult.class, aiClient, request);
    }

    public record InterrogationResult(
            String question,
            @JsonProperty("tts_question") String ttsQuestion,
            String reason,
            @JsonProperty("next_action") String nextAction,
            @JsonProperty("next_level") int nextLevel
    ) {
        public boolean isLevelUp() {
            return "LEVEL_UP".equalsIgnoreCase(nextAction);
        }

        public boolean isLevelStay() {
            return "LEVEL_STAY".equalsIgnoreCase(nextAction);
        }

        public boolean isChainSwitch() {
            return "CHAIN_SWITCH".equalsIgnoreCase(nextAction);
        }
    }
}
