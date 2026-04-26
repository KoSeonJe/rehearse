package com.rehearse.api.domain.interview.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;

import java.util.List;

public record TurnAnalysisResult(
        String answerText,
        IntentResult intent,
        AnswerAnalysis answerAnalysis
) {

    @JsonCreator
    public static TurnAnalysisResult fromJson(
            @JsonProperty("answer_text") String answerText,
            @JsonProperty("intent") IntentPayload intent,
            @JsonProperty("answer_analysis") AnswerAnalysis answerAnalysis
    ) {
        IntentResult ir = (intent != null)
                ? IntentResult.of(intent.type(), intent.confidence(), intent.reasoning())
                : IntentResult.forceAnswer();
        // intent != ANSWER 케이스에서 LLM 이 answer_analysis 키 누락/null 응답 시 502 방지.
        AnswerAnalysis fallback = (answerAnalysis != null)
                ? answerAnalysis
                : new AnswerAnalysis(0L, List.of(), List.of(), List.of(), 1, RecommendedNextAction.CLARIFICATION);
        return new TurnAnalysisResult(
                answerText != null ? answerText : "",
                ir,
                fallback
        );
    }

    public TurnAnalysisResult withAnswerAnalysis(AnswerAnalysis newAnalysis) {
        return new TurnAnalysisResult(answerText, intent, newAnalysis);
    }

    public record IntentPayload(
            @JsonProperty("type") IntentType type,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("reasoning") String reasoning
    ) {
        public IntentPayload {
            if (type == null) {
                type = IntentType.ANSWER;
            }
        }
    }
}
