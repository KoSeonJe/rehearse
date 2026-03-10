package com.devlens.api.domain.feedback.exception;

import com.devlens.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedbackErrorCode implements ErrorCode {
    INTERVIEW_NOT_COMPLETED(HttpStatus.CONFLICT, "FEEDBACK_001", "완료된 면접에서만 피드백을 생성할 수 있습니다."),
    SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FEEDBACK_002", "답변 데이터 직렬화에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
