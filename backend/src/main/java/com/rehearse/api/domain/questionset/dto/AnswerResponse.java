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
    private final String modelAnswer;
    private final String feedbackPerspective;
    private final long startMs;
    private final long endMs;

    public static AnswerResponse from(QuestionAnswer answer) {
        var question = answer.getQuestion();
        String perspective = question.getFeedbackPerspective() != null
                ? question.getFeedbackPerspective().name()
                : "TECHNICAL";

        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(question.getId())
                .questionType(question.getQuestionType().name())
                .questionText(question.getQuestionText())
                .modelAnswer(question.getModelAnswer())
                .feedbackPerspective(perspective)
                .startMs(answer.getStartMs())
                .endMs(answer.getEndMs())
                .build();
    }
}
