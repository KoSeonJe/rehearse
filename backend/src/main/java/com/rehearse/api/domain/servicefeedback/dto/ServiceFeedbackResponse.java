package com.rehearse.api.domain.servicefeedback.dto;

import com.rehearse.api.domain.servicefeedback.entity.FeedbackSource;
import com.rehearse.api.domain.servicefeedback.entity.ServiceFeedback;

import java.time.LocalDateTime;

public record ServiceFeedbackResponse(
        Long id,
        String content,
        Integer rating,
        FeedbackSource source,
        int completedCountSnapshot,
        LocalDateTime createdAt
) {
    public static ServiceFeedbackResponse from(ServiceFeedback feedback) {
        return new ServiceFeedbackResponse(
                feedback.getId(),
                feedback.getContent(),
                feedback.getRating(),
                feedback.getSource(),
                feedback.getCompletedCountSnapshot(),
                feedback.getCreatedAt()
        );
    }
}
