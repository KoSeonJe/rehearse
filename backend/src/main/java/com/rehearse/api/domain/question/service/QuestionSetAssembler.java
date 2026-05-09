package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import org.springframework.stereotype.Component;

@Component
public class QuestionSetAssembler {

    public QuestionSet fromPool(InterviewType type, QuestionPool pool) {
        return assemble(type, pool.getContent(), pool.getTtsContent(),
                pool.getBestAnswer(), pool);
    }

    public QuestionSet fromGenerated(GeneratedQuestion generated) {
        InterviewType category = resolveCategory(generated.questionCategory());
        return assemble(category, generated.content(), generated.ttsContent(),
                generated.bestAnswer(), null);
    }

    private QuestionSet assemble(InterviewType category, String questionText,
                                 String ttsText, String bestAnswer, QuestionPool poolRef) {
        QuestionSet qs = QuestionSet.builder()
                .category(category)
                .orderIndex(0)
                .build();

        Question question = Question.builder()
                .questionType(mainTypeOf(category))
                .questionText(questionText)
                .ttsText(ttsText)
                .bestAnswer(bestAnswer)
                .orderIndex(0)
                .questionPool(poolRef)
                .build();

        qs.addQuestion(question);
        return qs;
    }

    private QuestionType mainTypeOf(InterviewType category) {
        return category == InterviewType.BEHAVIORAL
                ? QuestionType.BEHAVIORAL_MAIN
                : QuestionType.TECH_MAIN;
    }

    private InterviewType resolveCategory(String questionCategory) {
        if (questionCategory == null) {
            throw new IllegalArgumentException("questionCategory must not be null");
        }
        if ("RESUME".equalsIgnoreCase(questionCategory)) {
            return InterviewType.RESUME_BASED;
        }
        try {
            return InterviewType.valueOf(questionCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown questionCategory: " + questionCategory, e);
        }
    }
}
