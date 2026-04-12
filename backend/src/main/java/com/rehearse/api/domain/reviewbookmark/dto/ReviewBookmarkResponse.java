package com.rehearse.api.domain.reviewbookmark.dto;

import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;

import java.time.LocalDateTime;

public record ReviewBookmarkResponse(
        Long id,
        Long timestampFeedbackId,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
    public static ReviewBookmarkResponse from(ReviewBookmark bookmark) {
        return new ReviewBookmarkResponse(
                bookmark.getId(),
                bookmark.getTimestampFeedback().getId(),
                bookmark.getResolvedAt(),
                bookmark.getCreatedAt()
        );
    }
}
