package com.rehearse.api.infra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.resume.question-generator")
public record OpenAiResumeQuestionGeneratorProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public OpenAiResumeQuestionGeneratorProperties {
        if (model == null || model.isBlank()) {
            model = "gpt-5.4-mini";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 120_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 16_000;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1/responses";
        }
    }
}
