package com.rehearse.api.domain.reviewbookmark.dto;

import java.util.List;

public record ReviewBookmarkListResponse(List<ReviewBookmarkListItem> items, int total) {

    public static ReviewBookmarkListResponse from(List<ReviewBookmarkListItem> items) {
        return new ReviewBookmarkListResponse(items, items.size());
    }
}
