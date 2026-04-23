package com.rehearse.api.domain.questionset.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AnalysisErrorCode implements ErrorCode {

    INVALID_ANALYSIS_STATUS_TRANSITION(HttpStatus.CONFLICT, "QUESTION_SET_002", "잘못된 분석 상태 전이입니다."),
    INVALID_CONVERT_STATUS_TRANSITION(HttpStatus.CONFLICT, "QUESTION_SET_006", "잘못된 변환 상태 전이입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
