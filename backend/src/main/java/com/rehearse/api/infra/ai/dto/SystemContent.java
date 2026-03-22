package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SystemContent {

    private final String type;
    private final String text;

    @JsonProperty("cache_control")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final CacheControl cacheControl;

    public static SystemContent of(String text) {
        return SystemContent.builder()
                .type("text")
                .text(text)
                .build();
    }

    public static SystemContent withCaching(String text) {
        return SystemContent.builder()
                .type("text")
                .text(text)
                .cacheControl(CacheControl.ephemeral())
                .build();
    }
}
