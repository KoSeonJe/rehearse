package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import org.springframework.stereotype.Component;

@Component
public class QuestionSetAssembler {

    public QuestionSet fromPool(InterviewType type, QuestionPool pool) {
        QuestionSetCategory category = QuestionSetCategory.valueOf(type.name());
        return assemble(category, pool.getContent(), pool.getTtsContent(),
                pool.getModelAnswer(), pool);
    }

    public QuestionSet fromGenerated(GeneratedQuestion generated) {
        QuestionSetCategory category = resolveCategory(generated.getQuestionCategory());
        return assemble(category, generated.getContent(), generated.getTtsContent(),
                generated.getModelAnswer(), null);
    }

    private QuestionSet assemble(QuestionSetCategory category, String questionText,
                                 String ttsText, String modelAnswer, QuestionPool poolRef) {
        QuestionSet qs = QuestionSet.builder()
                .category(category)
                .orderIndex(0)
                .build();

        Question question = Question.builder()
                .questionType(mainTypeOf(category))
                .questionText(questionText)
                .ttsText(ttsText)
                .modelAnswer(modelAnswer)
                .orderIndex(0)
                .questionPool(poolRef)
                .build();

        qs.addQuestion(question);
        return qs;
    }

    private QuestionType mainTypeOf(QuestionSetCategory category) {
        return category == QuestionSetCategory.BEHAVIORAL
                ? QuestionType.BEHAVIORAL_MAIN
                : QuestionType.TECH_MAIN;
    }

    private QuestionSetCategory resolveCategory(String questionCategory) {
        if (questionCategory == null) {
            throw new IllegalArgumentException("questionCategory must not be null");
        }
        if ("RESUME".equalsIgnoreCase(questionCategory)) {
            return QuestionSetCategory.RESUME_BASED;
        }
        try {
            return QuestionSetCategory.valueOf(questionCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown questionCategory: " + questionCategory, e);
        }
    }
}
