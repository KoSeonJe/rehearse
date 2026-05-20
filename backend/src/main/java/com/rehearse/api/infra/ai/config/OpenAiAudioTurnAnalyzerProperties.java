package com.rehearse.api.infra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.audio.turn-analyzer.openai")
public record OpenAiAudioTurnAnalyzerProperties(
        String model,
        long timeoutMs,
        int maxTokens,
        double temperature,
        String baseUrl
) {

    public OpenAiAudioTurnAnalyzerProperties {
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini-audio-preview";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
        if (maxTokens <= 0) {
            maxTokens = 1024;
        }
        if (temperature <= 0) {
            temperature = 0.2;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1/chat/completions";
        }
    }
}
