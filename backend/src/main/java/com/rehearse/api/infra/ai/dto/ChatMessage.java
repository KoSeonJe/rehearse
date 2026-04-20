package com.rehearse.api.infra.ai.dto;

/**
 * 범용 채팅 메시지 단위.
 *
 * @param role         메시지 발화자 역할
 * @param content      텍스트 콘텐츠
 * @param cacheControl true 이면 Claude prompt-caching ephemeral 마킹 대상
 */
public record ChatMessage(
        Role role,
        String content,
        boolean cacheControl
) {

    public enum Role {
        SYSTEM, USER, ASSISTANT
    }

    /** cacheControl = false 기본 생성자 */
    public static ChatMessage of(Role role, String content) {
        return new ChatMessage(role, content, false);
    }

    /** cacheControl = true (Claude ephemeral 마킹) */
    public static ChatMessage ofCached(Role role, String content) {
        return new ChatMessage(role, content, true);
    }

    /** role 소문자 문자열 반환 (OpenAI API 전송용) */
    public String roleLowercase() {
        return role.name().toLowerCase();
    }
}
