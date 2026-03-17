package com.rehearse.api.domain.report.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {
    NO_FEEDBACK(HttpStatus.CONFLICT, "REPORT_001", "피드백이 없어 리포트를 생성할 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_002", "아직 분석이 완료되지 않았습니다."),
    ANALYSIS_NOT_COMPLETED(HttpStatus.CONFLICT, "REPORT_003", "모든 질문세트 분석이 완료되지 않아 리포트를 생성할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
