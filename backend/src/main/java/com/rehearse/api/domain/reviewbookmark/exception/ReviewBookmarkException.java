package com.rehearse.api.domain.reviewbookmark.exception;

import com.rehearse.api.global.exception.BusinessException;

public class ReviewBookmarkException extends BusinessException {

    public ReviewBookmarkException(ReviewBookmarkErrorCode errorCode) {
        super(errorCode);
    }
}
