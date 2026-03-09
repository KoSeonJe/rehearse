package com.devlens.api.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FollowUpRequest {

    @NotBlank(message = "질문 내용을 입력해주세요.")
    private String questionContent;

    @NotBlank(message = "답변 텍스트를 입력해주세요.")
    private String answerText;

    private String nonVerbalSummary;
}
