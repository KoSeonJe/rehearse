package com.rehearse.api.infra.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.resume.question.openai")
public record OpenAiResumeQuestionProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public OpenAiResumeQuestionProperties {
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 14_800;
        }
        if (temperature <= 0) {
            temperature = 0.8;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1/chat/completions";
        }
    }
}
