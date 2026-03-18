package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.QuestionAnswer;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerResponse {

    private final Long id;
    private final Long questionId;
    private final String questionType;
    private final String questionText;
    private final long startMs;
    private final long endMs;

    public static AnswerResponse from(QuestionAnswer answer) {
        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion().getId())
                .questionType(answer.getQuestion().getQuestionType().name())
                .questionText(answer.getQuestion().getQuestionText())
                .startMs(answer.getStartMs())
                .endMs(answer.getEndMs())
                .build();
    }
}
