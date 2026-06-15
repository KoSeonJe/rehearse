package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rehearse.api.domain.question.entity.QuestionDepthType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedResumeQuestions(
        @JsonProperty("openers") List<GeneratedResumeQuestion> openers,
        @JsonProperty("mains") List<GeneratedResumeQuestion> mains
) {

    public GeneratedResumeQuestions {
        openers = openers != null ? List.copyOf(openers) : List.of();
        mains = mains != null ? List.copyOf(mains) : List.of();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedResumeQuestion(
            @JsonProperty("question") String question,
            @JsonProperty("tts_question") String ttsQuestion,
            @JsonProperty("best_answer") String bestAnswer,
            @JsonProperty("depth_type") QuestionDepthType depthType
    ) {
        public GeneratedResumeQuestion {
            if (question == null || question.isBlank()) {
                throw new IllegalArgumentException("question 필수");
            }
        }
    }
}
