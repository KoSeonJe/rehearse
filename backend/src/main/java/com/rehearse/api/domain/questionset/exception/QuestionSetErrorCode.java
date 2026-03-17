package com.rehearse.api.domain.questionset.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionSetErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_SET_001", "질문 세트를 찾을 수 없습니다."),
    INVALID_ANALYSIS_STATUS_TRANSITION(HttpStatus.CONFLICT, "QUESTION_SET_002", "잘못된 분석 상태 전이입니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_SET_003", "질문 세트 피드백을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
