package com.rehearse.api.domain.feedback.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveFeedbackRequest {

    @NotBlank(message = "코멘트는 필수입니다.")
    private String questionSetComment;

    @Valid
    private List<TimestampFeedbackItem> timestampFeedbacks;

    @JsonProperty("isVerbalCompleted")
    private boolean verbalCompleted;

    @JsonProperty("isNonverbalCompleted")
    private boolean nonverbalCompleted;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimestampFeedbackItem {
        private Long questionId;
        @NotNull private Long startMs;
        @NotNull private Long endMs;
        private String transcript;

        private CommentBlock nonverbalComment;
        private CommentBlock overallComment;
        private CommentBlock vocalComment;
        private CommentBlock attitudeComment;

        private Integer fillerWordCount;
        private String expressionLabel;

        // 3단계 라벨 (GOOD / AVERAGE / NEEDS_IMPROVEMENT)
        private String eyeContactLevel;
        private String postureLevel;
        private String toneConfidenceLevel;

        // 음성 특성
        private List<String> fillerWords;
        private String speechPace;
        private String emotionLabel;

        private NonverbalScore nonverbalScore;
        private String difficulty;
        private String resumeMode;
    }

    @Getter
    @NoArgsConstructor
    public static class NonverbalScore {
        private Integer fluency;
        @JsonProperty("confidence_tone")
        private Integer confidenceTone;
        @JsonProperty("eye_contact_posture")
        private Integer eyeContactPosture;
        private Integer composure;
        private Map<String, Object> rawSignals;
    }

    @Getter
    @NoArgsConstructor
    public static class CommentBlock {
        private String positive;
        private String negative;
        private String suggestion;
    }
}
