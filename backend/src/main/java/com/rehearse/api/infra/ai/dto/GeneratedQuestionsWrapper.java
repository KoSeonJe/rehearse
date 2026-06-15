package com.rehearse.api.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedQuestionsWrapper(
        List<GeneratedQuestion> questions
) {

    public GeneratedQuestionsWrapper {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "GeneratedQuestionsWrapper.questions 는 비어있을 수 없습니다.");
        }
        questions = List.copyOf(questions);
    }
}
