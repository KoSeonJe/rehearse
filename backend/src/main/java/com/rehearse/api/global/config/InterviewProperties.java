package com.rehearse.api.global.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@ConfigurationProperties(prefix = "rehearse.interview")
@Validated
public record InterviewProperties(
        @NotNull Retry retry,
        @NotNull Audio audio
) {

    public record Retry(
            @Min(1) int maxAttempts,
            @Min(0) int cooldownSeconds
    ) {
    }

    public record Audio(
            @Min(1) long maxBytes,
            @Min(1) int maxDurationSeconds,
            @NotEmpty Set<String> mimeWhitelist
    ) {
    }
}
