package com.rehearse.api.domain.feedback.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

        private Integer fillerWordCount;
        private List<String> fillerWords;

        @Valid
        private NonverbalScore nonverbalScore;
        private String difficulty;
        private String resumeMode;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NonverbalScore {
        @Valid private AreaScore vocal;
        @Valid private AreaScore vision;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AreaScore {
        @Valid private List<DimensionScoreItem> dimensions;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DimensionScoreItem {
        @JsonProperty("dimension_ref")
        @NotBlank
        private String dimensionRef;

        @NotNull
        @Min(1)
        @Max(3)
        private Integer score;

        @NotBlank
        private String observation;

        @JsonProperty("evidence_quote")
        @NotBlank
        private String evidenceQuote;
    }
}
