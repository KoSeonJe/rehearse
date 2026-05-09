package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

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
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> exchanges,
            String projectName, String chainTopic, int currentLevel, int answerQuality,
            String userAnswer, int consecutiveStayCount
    ) {
        return executeJson(
                CALL_TYPE, interviewId, state, exchanges,
                new FocusHints.ResumeChainInterrogatorHints(
                        projectName,
                        chainTopic,
                        currentLevel,
                        answerQuality,
                        userAnswer != null ? userAnswer : "",
                        consecutiveStayCount
                ),
                InterrogationResult.class
        );
    }

    public record InterrogationResult(
            String question,
            @JsonProperty("tts_question") String ttsQuestion,
            String reason,
            @JsonProperty("next_action") String nextAction,
            @JsonProperty("next_level") int nextLevel,
            @JsonProperty("best_answer") String bestAnswer
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

        public InterrogationResult withBestAnswer(String newBestAnswer) {
            return new InterrogationResult(question, ttsQuestion, reason, nextAction, nextLevel, newBestAnswer);
        }
    }
}
