package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedTurnAnalysis(
        @JsonProperty("answer_text") String answerText,
        @JsonProperty("answer_analysis") GeneratedAnswerAnalysis answerAnalysis
) {

    public GeneratedTurnAnalysis {
        answerText = answerText != null ? answerText : "";
    }

    public TurnAnalysisResult toDomain() {
        AnswerAnalysis analysis = answerAnalysis != null
                ? answerAnalysis.toDomain()
                : AnswerAnalysis.empty(0L);
        return new TurnAnalysisResult(answerText, analysis);
    }
}
