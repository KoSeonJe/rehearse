package com.devlens.api.domain.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerData {

    @NotNull(message = "질문 인덱스를 입력해주세요.")
    private Integer questionIndex;

    @NotBlank(message = "질문 내용을 입력해주세요.")
    private String questionContent;

    @NotBlank(message = "답변 텍스트를 입력해주세요.")
    private String answerText;

    private String nonVerbalSummary;

    private String voiceSummary;
}
