package com.rehearse.api.domain.resume.domain;

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
