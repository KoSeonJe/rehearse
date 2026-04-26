package com.rehearse.api.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("rehearse.context-engineering")
public record ContextEngineeringProperties(
        boolean l1Caching,
        @Min(2) @Max(20) int l3CompactionThreshold,
        @Min(1) @Max(10) int l3RecentWindow,
        boolean l4JustInTime,
        @Min(2000) @Max(32000) int maxContextTokens
) {
    public ContextEngineeringProperties {
        if (l3RecentWindow > l3CompactionThreshold) {
            throw new IllegalArgumentException("l3RecentWindow must be <= l3CompactionThreshold");
        }
    }
}
