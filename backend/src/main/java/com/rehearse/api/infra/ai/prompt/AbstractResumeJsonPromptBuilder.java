package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;

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

    protected <T> T executeJson(String callType, Map<String, String> variables, Class<T> resultClass) {
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                callType,
                Map.of(),
                List.of(),
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
}
