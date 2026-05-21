package com.rehearse.api.infra.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.rubric.scorer.claude")
public record ClaudeRubricScorerProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public ClaudeRubricScorerProperties {
        if (model == null || model.isBlank()) {
            model = "claude-sonnet-4-20250514";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 1536;
        }
        if (temperature <= 0) {
            temperature = 0.2;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.anthropic.com/v1/messages";
        }
    }
}
