package com.rehearse.api.domain.question.dto;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
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
    private final String rubricCategory;
    private final String difficulty;
    private final long startMs;
    private final long endMs;

    public static AnswerResponse from(QuestionAnswer answer, InterviewType category) {
        var question = answer.getQuestion();
        String perspective = question.getQuestionType()
                .rubricCategory()
                .name();

        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(question.getId())
                .questionType(question.getQuestionType().name())
                .questionText(question.getQuestionText())
                .modelAnswer(question.getModelAnswer())
                .rubricCategory(perspective)
                .difficulty("easy")
                .startMs(answer.getStartMs())
                .endMs(answer.getEndMs())
                .build();
    }
}
