package com.rehearse.api.domain.interview.entity;

public record TurnAnalysisResult(
        String answerText,
        AnswerAnalysis answerAnalysis
) {

    public TurnAnalysisResult withAnswerAnalysis(AnswerAnalysis newAnalysis) {
        return new TurnAnalysisResult(answerText, newAnalysis);
    }
}
