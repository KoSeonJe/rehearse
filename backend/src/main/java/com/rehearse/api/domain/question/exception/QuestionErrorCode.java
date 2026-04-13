package com.rehearse.api.domain.question.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionErrorCode implements ErrorCode {

    MAX_FOLLOWUP_EXCEEDED(HttpStatus.BAD_REQUEST, "QUESTION_SET_004", "후속 질문은 최대 2개까지 생성할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
