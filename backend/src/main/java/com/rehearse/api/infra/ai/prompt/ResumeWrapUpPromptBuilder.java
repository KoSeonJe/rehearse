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
public class ResumeWrapUpPromptBuilder extends AbstractResumeJsonPromptBuilder {

    private static final String CALL_TYPE = "resume_wrap_up";

    public ResumeWrapUpPromptBuilder(
            AiClient aiClient,
            AiResponseParser aiResponseParser,
            InterviewContextBuilder contextBuilder,
            @Value("${rehearse.resume-track.model:gpt-4o-mini}") String model,
            @Value("${rehearse.resume-track.temperature:0.7}") double temperature,
            @Value("${rehearse.resume-track.max-tokens:800}") int maxTokens
    ) {
        super(aiClient, aiResponseParser, contextBuilder, model, temperature, maxTokens);
    }

    public WrapUpResult build(
            Long interviewId, InterviewRuntimeState state, List<FollowUpExchange> exchanges,
            String sessionSummary, long remainingMinutes, boolean isRetrospective
    ) {
        return executeJson(
                CALL_TYPE, interviewId, state, exchanges,
                new FocusHints.ResumeWrapUpHints(
                        sessionSummary != null ? sessionSummary : "",
                        remainingMinutes,
                        isRetrospective
                ),
                WrapUpResult.class
        );
    }

    public record WrapUpResult(
            String question,
            @JsonProperty("tts_question") String ttsQuestion,
            String reason,
            @JsonProperty("is_wrap_up_question") boolean isWrapUpQuestion,
            @JsonProperty("session_complete") boolean sessionComplete
    ) {}
}
