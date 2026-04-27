package com.rehearse.api.domain.resume.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResumeErrorCode implements ErrorCode {
    INVALID_FILE_EMPTY(HttpStatus.BAD_REQUEST, "RESUME_001", "이력서 파일이 비어 있습니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "RESUME_002", "이력서 파일 크기는 5MB를 초과할 수 없습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "RESUME_003", "PDF 파일만 업로드할 수 있습니다."),
    INVALID_FILE_MAGIC_BYTES(HttpStatus.BAD_REQUEST, "RESUME_004", "유효한 PDF 파일이 아닙니다."),
    EMPTY_RESUME_TEXT(HttpStatus.UNPROCESSABLE_ENTITY, "RESUME_005", "이력서에서 텍스트를 추출할 수 없습니다. PDF에 충분한 텍스트가 포함되어 있는지 확인해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
