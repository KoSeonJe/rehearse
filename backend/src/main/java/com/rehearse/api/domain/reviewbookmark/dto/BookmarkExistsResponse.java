package com.rehearse.api.domain.reviewbookmark.dto;

import java.util.List;

public record BookmarkExistsResponse(
        List<BookmarkIdPair> items
) {}
