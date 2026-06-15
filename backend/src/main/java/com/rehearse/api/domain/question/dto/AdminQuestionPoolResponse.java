package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.question.entity.QuestionPool;

import java.time.LocalDateTime;

public record AdminQuestionPoolResponse(
        Long id,
        String cacheKey,
        String content,
        String ttsContent,
        String category,
        String bestAnswer,
        boolean isActive,
        LocalDateTime createdAt
) {

    public static AdminQuestionPoolResponse from(QuestionPool questionPool) {
        return new AdminQuestionPoolResponse(
                questionPool.getId(),
                questionPool.getCacheKey(),
                questionPool.getContent(),
                questionPool.getTtsContent(),
                questionPool.getCategory(),
                questionPool.getBestAnswer(),
                questionPool.isActive(),
                questionPool.getCreatedAt());
    }
}
