package com.rehearse.api.domain.questionset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
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
    public static class TimestampFeedbackItem {
        private Long questionId;
        @NotNull private Long startMs;
        @NotNull private Long endMs;
        private String transcript;

        // feedback-v3: ✓△→ 단일 String → 정형 객체
        private CommentBlock verbalComment;
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

        // 기술 피드백
        private String accuracyIssues;  // JSON: [{"claim":"...","correction":"..."}]
        private String coachingStructure;
        private String coachingImprovement;
    }

    @Getter
    @NoArgsConstructor
    public static class CommentBlock {
        private String positive;
        private String negative;
        private String suggestion;
    }
}
