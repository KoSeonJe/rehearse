package com.rehearse.api.infra.ai.dto;

public record ChatMessage(
        Role role,
        String content,
        boolean cacheControl
) {

    public enum Role {
        SYSTEM, USER, ASSISTANT
    }

    public static ChatMessage of(Role role, String content) {
        return new ChatMessage(role, content, false);
    }

    public static ChatMessage ofCached(Role role, String content) {
        return new ChatMessage(role, content, true);
    }

    public String roleLowercase() {
        return role.name().toLowerCase();
    }
}
