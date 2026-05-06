package com.rehearse.api.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final ErrorCode errorCode;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.errorCode = null;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getStatus();
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }
}
