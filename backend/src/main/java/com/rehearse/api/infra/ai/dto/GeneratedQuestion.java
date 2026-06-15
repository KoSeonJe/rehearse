package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedQuestion(
        String content,
        @JsonProperty("tts_content") String ttsContent,
        String category,
        int order,
        @JsonProperty("evaluation_criteria") String evaluationCriteria,
        @JsonProperty("question_category") String questionCategory,
        @JsonProperty("best_answer") String bestAnswer,
        String followUpStrategy
) {

    public GeneratedQuestion {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "GeneratedQuestion.content 는 비어있을 수 없습니다.");
        }
        if (questionCategory == null || questionCategory.isBlank()) {
            throw new IllegalArgumentException(
                    "GeneratedQuestion.questionCategory 는 비어있을 수 없습니다.");
        }
    }
}
