package com.rehearse.api.domain.questionset.dto;

import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnswer;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class QuestionsWithAnswersResponse {

    private final List<QuestionWithAnswer> questions;

    @Getter
    @Builder
    public static class QuestionWithAnswer {
        private final Long questionId;
        private final String questionType;
        private final String questionText;
        private final String modelAnswer;
        private final Long startMs;
        private final Long endMs;
    }

    public static QuestionsWithAnswersResponse from(List<Question> questions,
                                                      List<QuestionSetAnswer> answers) {
        Map<Long, QuestionSetAnswer> answerByQuestionId = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        List<QuestionWithAnswer> items = questions.stream()
                .map(q -> {
                    QuestionSetAnswer answer = answerByQuestionId.get(q.getId());
                    return QuestionWithAnswer.builder()
                            .questionId(q.getId())
                            .questionType(q.getQuestionType().name())
                            .questionText(q.getQuestionText())
                            .modelAnswer(q.getModelAnswer())
                            .startMs(answer != null ? answer.getStartMs() : null)
                            .endMs(answer != null ? answer.getEndMs() : null)
                            .build();
                })
                .toList();

        return QuestionsWithAnswersResponse.builder()
                .questions(items)
                .build();
    }
}
