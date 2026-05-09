package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedTurnAnalysis(
        @JsonProperty("answer_text") String answerText,
        @JsonProperty("answer_analysis") GeneratedAnswerAnalysis answerAnalysis
) {

    public GeneratedTurnAnalysis {
        if (answerAnalysis == null) {
            throw new IllegalArgumentException(
                    "GeneratedTurnAnalysis.answerAnalysis 는 null 일 수 없습니다.");
        }
        answerText = answerText != null ? answerText : "";
    }

    public TurnAnalysisResult toDomain() {
        return new TurnAnalysisResult(answerText, answerAnalysis.toDomain());
    }
}
