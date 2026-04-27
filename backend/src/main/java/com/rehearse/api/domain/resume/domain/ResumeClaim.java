package com.rehearse.api.domain.resume.domain;

import java.util.List;

public record ResumeClaim(
        String claimId,
        String text,
        ClaimType claimType,
        Priority priority,
        List<String> depthHooks
) {

    public enum ClaimType {
        PROBLEM_SOLVING,
        ARCHITECTURE_CHOICE,
        IMPLEMENTATION,
        IMPACT_METRIC;

        public static ClaimType fromOrDefault(String value, ClaimType defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return defaultValue;
            }
        }
    }

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW;

        public static Priority fromOrDefault(String value, Priority defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            return switch (value.toLowerCase()) {
                case "high" -> HIGH;
                case "low" -> LOW;
                default -> defaultValue;
            };
        }
    }
}
