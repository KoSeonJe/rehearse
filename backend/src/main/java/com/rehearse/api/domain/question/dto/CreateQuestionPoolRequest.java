package com.rehearse.api.domain.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionPoolRequest(
        @NotBlank(message = "캐시 키는 필수입니다.")
        @Size(max = 255, message = "캐시 키는 255자 이하여야 합니다.")
        String cacheKey,

        @NotBlank(message = "질문 본문은 필수입니다.")
        String content,

        String ttsContent,

        @Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
        String category,

        String bestAnswer
) {
}
