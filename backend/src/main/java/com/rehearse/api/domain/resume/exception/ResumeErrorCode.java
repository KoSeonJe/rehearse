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
    EMPTY_RESUME_TEXT(HttpStatus.UNPROCESSABLE_ENTITY, "RESUME_005", "이력서에서 텍스트를 추출할 수 없습니다. PDF에 충분한 텍스트가 포함되어 있는지 확인해주세요."),
    RESUME_EXCLUSIVITY_VIOLATION(HttpStatus.BAD_REQUEST, "RESUME_006", "이력서 면접(RESUME_BASED)은 단독으로만 선택할 수 있습니다. 다른 면접 유형과 함께 선택할 수 없습니다."),
    RESUME_REQUIRED_FOR_RESUME_BASED(HttpStatus.BAD_REQUEST, "RESUME_007", "RESUME_BASED 면접 유형을 선택하려면 이력서 파일을 첨부해야 합니다."),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_008", "면접 플랜을 찾을 수 없습니다."),
    PROJECT_NOT_FOUND_IN_SKELETON(HttpStatus.INTERNAL_SERVER_ERROR, "RESUME_009", "Skeleton에서 projectId 에 해당하는 프로젝트를 찾을 수 없습니다."),
    INTERVIEW_PLAN_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "RESUME_010", "이미 interview 에 할당된 plan 입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
