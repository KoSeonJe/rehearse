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
    NOT_IN_PROGRESS(HttpStatus.CONFLICT, "INTERVIEW_003", "진행 중인 면접에서만 후속 질문을 생성할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
