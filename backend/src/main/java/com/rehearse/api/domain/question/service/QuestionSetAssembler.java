package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import org.springframework.stereotype.Component;

@Component
public class QuestionSetAssembler {

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

    public QuestionSet fromPool(InterviewType type, QuestionPool pool) {
        return assemble(type, pool.getContent(), pool.getTtsContent(),
                pool.getBestAnswer(), pool);
    }

    private QuestionType mainTypeOf(InterviewType category) {
        return category == InterviewType.BEHAVIORAL
                ? QuestionType.BEHAVIORAL_MAIN
                : QuestionType.TECH_MAIN;
    }
}
