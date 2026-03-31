package com.rehearse.api.domain.interview.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "INTERVIEW_002", "잘못된 상태 전이입니다."),
    NOT_IN_PROGRESS(HttpStatus.CONFLICT, "INTERVIEW_003", "진행 중인 면접에서만 후속 질문을 생성할 수 있습니다."),
    QUESTION_GENERATION_NOT_COMPLETED(HttpStatus.CONFLICT, "INTERVIEW_004", "질문 생성이 완료되지 않아 면접을 시작할 수 없습니다."),
    QUESTION_GENERATION_NOT_FAILED(HttpStatus.CONFLICT, "INTERVIEW_005", "실패 상태의 질문 생성만 재시도할 수 있습니다."),
    ANSWER_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "INTERVIEW_006", "답변 텍스트 또는 오디오 파일이 필요합니다."),
    INVALID_TECH_STACK(HttpStatus.BAD_REQUEST, "INTERVIEW_007", "해당 직무에서 지원하지 않는 기술 스택입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "INTERVIEW_008", "해당 면접에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
