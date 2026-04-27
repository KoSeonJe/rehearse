package com.rehearse.api.domain.resume.domain;

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
