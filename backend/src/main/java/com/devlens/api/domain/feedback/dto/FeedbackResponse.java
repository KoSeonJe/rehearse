package com.devlens.api.domain.feedback.dto;

import com.devlens.api.domain.feedback.entity.Feedback;
import com.devlens.api.domain.feedback.entity.FeedbackCategory;
import com.devlens.api.domain.feedback.entity.FeedbackSeverity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedbackResponse {

    private final Long id;
    private final double timestampSeconds;
    private final FeedbackCategory category;
    private final FeedbackSeverity severity;
    private final String content;
    private final String suggestion;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .timestampSeconds(feedback.getTimestampSeconds())
                .category(feedback.getCategory())
                .severity(feedback.getSeverity())
                .content(feedback.getContent())
                .suggestion(feedback.getSuggestion())
                .build();
    }
}
