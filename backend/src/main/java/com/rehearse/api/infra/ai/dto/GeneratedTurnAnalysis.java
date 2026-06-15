package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedTurnAnalysis(
        @JsonProperty("answer_analysis") GeneratedAnswerAnalysis answerAnalysis
) {

    public AnswerAnalysis toDomain() {
        return answerAnalysis != null ? answerAnalysis.toDomain() : AnswerAnalysis.empty();
    }
}
