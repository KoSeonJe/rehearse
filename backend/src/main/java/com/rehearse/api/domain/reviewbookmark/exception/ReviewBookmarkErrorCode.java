package com.rehearse.api.domain.reviewbookmark.exception;

import com.rehearse.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewBookmarkErrorCode implements ErrorCode {
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_BOOKMARK_001", "북마크를 찾을 수 없습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_BOOKMARK_002", "이미 담긴 답변입니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "REVIEW_BOOKMARK_003", "본인의 북마크만 조작할 수 있습니다."),
    TIMESTAMP_FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_BOOKMARK_004", "대상 피드백을 찾을 수 없습니다."),
    INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "REVIEW_BOOKMARK_005", "허용되지 않은 상태 필터입니다."),
    TOO_MANY_IDS(HttpStatus.BAD_REQUEST, "REVIEW_BOOKMARK_006", "한 번에 조회 가능한 피드백 수를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
