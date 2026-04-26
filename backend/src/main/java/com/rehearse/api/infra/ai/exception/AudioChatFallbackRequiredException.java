package com.rehearse.api.infra.ai.exception;

/** caller MUST catch and route to text-only fallback. */
public class AudioChatFallbackRequiredException extends RuntimeException {

    public AudioChatFallbackRequiredException(String message) {
        super(message);
    }

    public AudioChatFallbackRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
