package com.rehearse.api.infra.ai.dto.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaudeResponse {

    private String id;
    private String type;
    private String model;
    private String role;
    private List<Content> content;

    @JsonProperty("stop_reason")
    private String stopReason;

    private Usage usage;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private String type;
        private String text;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("input_tokens")
        private int inputTokens;

        @JsonProperty("output_tokens")
        private int outputTokens;

        @JsonProperty("cache_creation_input_tokens")
        private int cacheCreationInputTokens;

        @JsonProperty("cache_read_input_tokens")
        private int cacheReadInputTokens;
    }
}
