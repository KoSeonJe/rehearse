package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractResumeJsonPromptBuilder {

    protected final AiClient aiClient;
    protected final AiResponseParser aiResponseParser;
    protected final InterviewContextBuilder contextBuilder;
    protected final String model;
    protected final double temperature;
    protected final int maxTokens;

    protected AbstractResumeJsonPromptBuilder(
            AiClient aiClient,
            AiResponseParser aiResponseParser,
            InterviewContextBuilder contextBuilder,
            String model,
            double temperature,
            int maxTokens
    ) {
        this.aiClient = aiClient;
        this.aiResponseParser = aiResponseParser;
        this.contextBuilder = contextBuilder;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    protected <T> T executeJson(String callType, Map<String, Object> variables, Class<T> resultClass) {
        return executeJson(callType, null, null, null, variables, resultClass);
    }

    protected <T> T executeJson(
            String callType,
            Long interviewId,
            InterviewRuntimeState runtimeState,
            List<FollowUpExchange> exchanges,
            Map<String, Object> variables,
            Class<T> resultClass
    ) {
        Map<String, Object> runtimeStateMap = buildRuntimeStateMap(interviewId, runtimeState);
        List<FollowUpExchange> safeExchanges = exchanges != null ? exchanges : List.of();

        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                callType,
                runtimeStateMap,
                safeExchanges,
                variables,
                null
        ));

        ChatRequest request = ChatRequest.builder()
                .messages(built.messages())
                .callType(callType)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(request);
        return aiResponseParser.parseOrRetry(response, resultClass, aiClient, request);
    }

    private static Map<String, Object> buildRuntimeStateMap(Long interviewId, InterviewRuntimeState state) {
        if (interviewId == null && state == null) {
            return Map.of();
        }
        Map<String, Object> map = new HashMap<>();
        if (state != null) {
            map.put("interviewRuntimeState", state);
        }
        if (interviewId != null) {
            map.put("interviewId", interviewId);
        }
        return map;
    }
}
