package com.rehearse.api.domain.interview.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TurnAnalysisResult(
        String answerText,
        AnswerAnalysis answerAnalysis
) {

    @JsonCreator
    public static TurnAnalysisResult fromJson(
            @JsonProperty("answer_text") String answerText,
            @JsonProperty("answer_analysis") AnswerAnalysis answerAnalysis
    ) {
        AnswerAnalysis fallback = (answerAnalysis != null)
                ? answerAnalysis
                : new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 1, RecommendedNextAction.CLARIFICATION);
        return new TurnAnalysisResult(
                answerText != null ? answerText : "",
                fallback
        );
    }

    public TurnAnalysisResult withAnswerAnalysis(AnswerAnalysis newAnalysis) {
        return new TurnAnalysisResult(answerText, newAnalysis);
    }
}
