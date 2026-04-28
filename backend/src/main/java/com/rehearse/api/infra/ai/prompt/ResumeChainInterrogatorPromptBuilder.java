package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ResumeChainInterrogatorPromptBuilder extends AbstractResumeJsonPromptBuilder {

    private static final String CALL_TYPE = "resume_chain_interrogator";

    public ResumeChainInterrogatorPromptBuilder(
            AiClient aiClient,
            AiResponseParser aiResponseParser,
            InterviewContextBuilder contextBuilder,
            @Value("${rehearse.resume-track.model:gpt-4o-mini}") String model,
            @Value("${rehearse.resume-track.temperature:0.7}") double temperature,
            @Value("${rehearse.resume-track.max-tokens:800}") int maxTokens
    ) {
        super(aiClient, aiResponseParser, contextBuilder, model, temperature, maxTokens);
    }

    public InterrogationResult build(
            String chainTopic, int currentLevel, int answerQuality,
            String userAnswer, int consecutiveStayCount
    ) {
        return executeJson(CALL_TYPE, Map.of(
                "CURRENT_CHAIN", chainTopic,
                "CURRENT_LEVEL", String.valueOf(currentLevel),
                "ANSWER_QUALITY", String.valueOf(answerQuality),
                "USER_ANSWER", userAnswer != null ? userAnswer : "",
                "CONSECUTIVE_STAY_COUNT", String.valueOf(consecutiveStayCount)
        ), InterrogationResult.class);
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
