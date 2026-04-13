package com.rehearse.api.domain.feedback.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedbackErrorCode implements ErrorCode {

    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_SET_003", "질문 세트 피드백을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
