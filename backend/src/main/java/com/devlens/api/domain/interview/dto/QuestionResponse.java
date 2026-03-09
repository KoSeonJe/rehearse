package com.devlens.api.domain.interview.dto;

import com.devlens.api.domain.interview.entity.InterviewQuestion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionResponse {

    private final Long id;
    private final String content;
    private final String category;
    private final int order;

    public static QuestionResponse from(InterviewQuestion question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory())
                .order(question.getQuestionOrder())
                .build();
    }
}
