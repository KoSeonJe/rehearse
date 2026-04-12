package com.rehearse.api.domain.reviewbookmark.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReviewBookmarkRequest(
        @NotNull @Positive(message = "타임스탬프 피드백 ID는 양수여야 합니다.")
        Long timestampFeedbackId
) {}
