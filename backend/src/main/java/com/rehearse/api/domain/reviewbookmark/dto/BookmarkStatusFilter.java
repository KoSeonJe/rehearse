package com.rehearse.api.domain.reviewbookmark.dto;

import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkErrorCode;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException;

public enum BookmarkStatusFilter {
    ALL,
    IN_PROGRESS,
    RESOLVED;

    public static BookmarkStatusFilter from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return BookmarkStatusFilter.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReviewBookmarkException(ReviewBookmarkErrorCode.INVALID_STATUS_FILTER);
        }
    }
}
