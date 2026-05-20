package com.rehearse.api.infra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.answer.analyzer.openai")
public record OpenAiAnswerAnalyzerProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public OpenAiAnswerAnalyzerProperties {
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 800;
        }
        if (temperature <= 0) {
            temperature = 0.2;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1/chat/completions";
        }
    }
}
