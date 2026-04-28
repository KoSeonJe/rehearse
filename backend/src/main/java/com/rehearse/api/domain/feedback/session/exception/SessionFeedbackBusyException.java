package com.rehearse.api.domain.feedback.session.exception;

import com.rehearse.api.global.exception.BusinessException;

public class SessionFeedbackBusyException extends BusinessException {

    public SessionFeedbackBusyException() {
        super(SessionFeedbackErrorCode.BUSY);
    }
}
