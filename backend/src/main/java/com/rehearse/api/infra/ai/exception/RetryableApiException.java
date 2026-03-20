package com.rehearse.api.infra.ai.exception;

public class RetryableApiException extends RuntimeException {
    public RetryableApiException(String message) {
        super(message);
    }
}
