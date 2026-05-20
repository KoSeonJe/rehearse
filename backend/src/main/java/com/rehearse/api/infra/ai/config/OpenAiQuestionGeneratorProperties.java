package com.rehearse.api.infra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.question.generator.openai")
public record OpenAiQuestionGeneratorProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public OpenAiQuestionGeneratorProperties {
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 8192;
        }
        if (temperature <= 0) {
            temperature = 0.9;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1/chat/completions";
        }
    }
}
