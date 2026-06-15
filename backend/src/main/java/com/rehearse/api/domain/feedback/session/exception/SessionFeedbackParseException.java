package com.rehearse.api.domain.feedback.session.exception;

import com.rehearse.api.global.exception.BusinessException;

public class SessionFeedbackParseException extends BusinessException {

    public SessionFeedbackParseException(String reason) {
        super(SessionFeedbackErrorCode.PARSE_FAILED.getStatus(),
                SessionFeedbackErrorCode.PARSE_FAILED.getCode(),
                "피드백 파싱 실패: " + reason);
    }
}
