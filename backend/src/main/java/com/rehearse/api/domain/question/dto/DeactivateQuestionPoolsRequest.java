package com.rehearse.api.domain.question.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeactivateQuestionPoolsRequest(
        @NotEmpty(message = "비활성화할 질문 풀 ID는 필수입니다.")
        List<Long> ids
) {
}
