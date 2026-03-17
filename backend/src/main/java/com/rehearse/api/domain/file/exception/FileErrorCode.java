package com.rehearse.api.domain.file.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_001", "파일 메타데이터를 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "FILE_002", "잘못된 파일 상태 전이입니다."),
    S3_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_003", "해당 S3 키의 파일을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
