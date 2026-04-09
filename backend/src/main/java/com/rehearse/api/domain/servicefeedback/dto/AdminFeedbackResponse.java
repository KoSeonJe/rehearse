package com.rehearse.api.domain.servicefeedback.dto;

import com.rehearse.api.domain.servicefeedback.entity.FeedbackSource;

import java.time.LocalDateTime;

public record AdminFeedbackResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String content,
        Integer rating,
        FeedbackSource source,
        int completedCountSnapshot,
        LocalDateTime createdAt
) {}
