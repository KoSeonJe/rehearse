package com.rehearse.api.domain.servicefeedback.dto;

import com.rehearse.api.domain.servicefeedback.entity.FeedbackSource;
import jakarta.validation.constraints.*;

public record CreateServiceFeedbackRequest(
        @NotBlank @Size(min = 10) String content,
        @Min(1) @Max(5) Integer rating,
        @NotNull FeedbackSource source
) {}
