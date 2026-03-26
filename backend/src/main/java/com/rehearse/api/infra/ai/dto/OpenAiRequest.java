package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OpenAiRequest {

    private final String model;

    private final List<Message> messages;

    @JsonProperty("max_tokens")
    private final int maxTokens;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Double temperature;

    @Getter
    @Builder
    public static class Message {
        private final String role;
        private final String content;
    }
}
