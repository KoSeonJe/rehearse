package com.rehearse.api.infra.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.followup.generator.claude")
public record ClaudeFollowUpQuestionGeneratorProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public ClaudeFollowUpQuestionGeneratorProperties {
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-4-20250514";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 1024;
        }
        if (temperature <= 0) {
            temperature = 0.6;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.anthropic.com/v1/messages";
        }
    }
}
