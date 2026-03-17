package com.rehearse.api.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String code;
    private final String message;
    private final List<FieldError> errors;
    private final LocalDateTime timestamp;

    public static ErrorResponse of(int status, String code, String message) {
        return ErrorResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(int status, String code, String message, List<FieldError> errors) {
        return ErrorResponse.builder()
                .status(status)
                .code(code)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String value;
        private final String reason;
    }
}
