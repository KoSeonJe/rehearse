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

    // Gemini 네이티브 오디오 분석 종합 리포트 필드 (nullable — 기존 API 호환)
    private String verbalSummary;
    private String vocalSummary;
    private String nonverbalSummary;
    private List<String> strengths;
    private List<String> improvements;
    private String topPriorityAdvice;

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

        // Gemini 네이티브 오디오 분석 음성 특성 필드 (nullable — 기존 API 호환)
        private List<String> fillerWords;
        private String speechPace;
        private Integer toneConfidence;
        private String emotionLabel;
        private String vocalComment;
    }
}
