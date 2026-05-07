package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerResponse {

    private final Long id;
    private final Long questionId;
    private final String questionType;
    private final String questionText;
    private final String modelAnswer;
    private final String feedbackPerspective;
    private final String difficulty;
    private final long startMs;
    private final long endMs;

    public static AnswerResponse from(QuestionAnswer answer, QuestionSetCategory category) {
        var question = answer.getQuestion();
        String perspective = question.getQuestionType()
                .feedbackPerspective()
                .name();

        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(question.getId())
                .questionType(question.getQuestionType().name())
                .questionText(question.getQuestionText())
                .modelAnswer(question.getModelAnswer())
                .feedbackPerspective(perspective)
                .difficulty("easy")
                .startMs(answer.getStartMs())
                .endMs(answer.getEndMs())
                .build();
    }
}
