package com.rehearse.api.infra.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiCommonProperties(
        String apiKey
) {

    public OpenAiCommonProperties {
        if (apiKey == null) {
            apiKey = "";
        }
    }
}
