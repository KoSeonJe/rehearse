package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.ReferenceType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionDetailResponse {

    private final Long id;
    private final QuestionType questionType;
    private final String questionText;
    private final String modelAnswer;
    private final ReferenceType referenceType;
    private final int orderIndex;

    public static QuestionDetailResponse from(Question question) {
        return QuestionDetailResponse.builder()
                .id(question.getId())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .modelAnswer(question.getModelAnswer())
                .referenceType(question.getReferenceType())
                .orderIndex(question.getOrderIndex())
                .build();
    }
}
