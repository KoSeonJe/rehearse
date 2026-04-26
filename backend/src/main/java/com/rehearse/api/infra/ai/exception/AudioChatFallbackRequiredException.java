package com.rehearse.api.infra.ai.exception;

/**
 * audio chat 경로가 retryable 인프라 오류로 실패했을 때 ResilientAiClient 가 던진다.
 * caller(AudioTurnAnalyzer)는 이 예외만 catch 해 STT + 텍스트 fallback 경로로 전환한다.
 * AiErrorCode 세부 사항을 caller 에 노출하지 않도록 추상화 경계를 제공한다.
 */
public class AudioChatFallbackRequiredException extends RuntimeException {

    public AudioChatFallbackRequiredException(String message) {
        super(message);
    }

    public AudioChatFallbackRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
