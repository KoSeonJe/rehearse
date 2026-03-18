package com.rehearse.api.domain.questionset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SaveFeedbackRequest {

    @NotNull(message = "점수는 필수입니다.")
    private Integer questionSetScore;

    @NotBlank(message = "코멘트는 필수입니다.")
    private String questionSetComment;

    @Valid
    private List<TimestampFeedbackItem> timestampFeedbacks;

    @Getter
    @NoArgsConstructor
    public static class TimestampFeedbackItem {
        private Long questionId;
        @NotNull private Long startMs;
        @NotNull private Long endMs;
        private String transcript;
        private Integer verbalScore;
        private String verbalComment;
        private Integer fillerWordCount;
        private Integer eyeContactScore;
        private Integer postureScore;
        private String expressionLabel;
        private String nonverbalComment;
        private String overallComment;
    }
}
