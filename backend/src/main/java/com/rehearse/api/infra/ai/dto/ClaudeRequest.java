package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClaudeRequest {

    private final String model;

    @JsonProperty("max_tokens")
    private final int maxTokens;

    private final String system;

    private final List<Message> messages;

    private final Double temperature;

    @Getter
    @Builder
    public static class Message {
        private final String role;
        private final String content;
    }
}
