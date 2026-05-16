package com.rehearse.api.domain.question.dto;

public record AdminQuestionPoolSearchCondition(
        String cacheKey,
        String category,
        Boolean isActive,
        String keyword
) {
}
