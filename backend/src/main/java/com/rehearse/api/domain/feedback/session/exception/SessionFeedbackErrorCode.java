package com.rehearse.api.domain.feedback.session.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionFeedbackErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_FEEDBACK_001", "세션 피드백을 찾을 수 없습니다."),
    ALREADY_EXISTS(HttpStatus.CONFLICT, "SESSION_FEEDBACK_002", "이미 피드백이 존재합니다."),
    BUSY(HttpStatus.CONFLICT, "SESSION_FEEDBACK_003", "이미 재시도가 진행 중입니다."),
    PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SESSION_FEEDBACK_004", "피드백 파싱에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
