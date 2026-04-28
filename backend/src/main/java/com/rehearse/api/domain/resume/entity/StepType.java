package com.rehearse.api.domain.resume.entity;

public enum StepType {
    WHAT,
    HOW,
    WHY_MECH,
    TRADEOFF;

    public static StepType fromOrDefault(String value, StepType defaultValue) {
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
