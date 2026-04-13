package com.rehearse.api.domain.questionset.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionSetErrorCode implements ErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_SET_001", "질문 세트를 찾을 수 없습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION_SET_005", "질문 세트에 연결된 파일이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
