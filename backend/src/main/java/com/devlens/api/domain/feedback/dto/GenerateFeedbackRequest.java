package com.devlens.api.domain.feedback.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GenerateFeedbackRequest {

    @NotEmpty(message = "답변 데이터가 필요합니다.")
    @Valid
    private List<AnswerData> answers;
}
