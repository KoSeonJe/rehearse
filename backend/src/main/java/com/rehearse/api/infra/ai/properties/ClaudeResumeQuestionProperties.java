package com.rehearse.api.infra.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.resume.question.claude")
public record ClaudeResumeQuestionProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public ClaudeResumeQuestionProperties {
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-4-20250514";
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
            baseUrl = "https://api.anthropic.com/v1/messages";
        }
    }
}
