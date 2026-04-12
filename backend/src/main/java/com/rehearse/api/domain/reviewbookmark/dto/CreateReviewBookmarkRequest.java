package com.rehearse.api.domain.reviewbookmark.dto;

import jakarta.validation.constraints.NotNull;

public record CreateReviewBookmarkRequest(
        @NotNull Long timestampFeedbackId
) {}
