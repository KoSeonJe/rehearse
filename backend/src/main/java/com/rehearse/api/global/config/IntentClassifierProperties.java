package com.rehearse.api.global.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rehearse.intent-classifier")
@Validated
public record IntentClassifierProperties(
        @DecimalMin("0.0") @DecimalMax("1.0") double fallbackOnLowConfidence,
        @Min(1) int offTopicConsecutiveLimit
) {}
