package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import org.springframework.stereotype.Component;

@Component
public class QuestionSetAssembler {

    public QuestionSet fromPool(InterviewType type, QuestionPool pool) {
        return assemble(
                QuestionSetCategory.valueOf(type.name()),
                pool.getContent(), pool.getTtsContent(), pool.getModelAnswer(),
                pool.getReferenceType(), perspectiveOf(type), pool
        );
    }

    public QuestionSet fromGenerated(GeneratedQuestion generated) {
        QuestionSetCategory category = resolveCategory(generated.getQuestionCategory());
        return assemble(
                category, generated.getContent(), generated.getTtsContent(), generated.getModelAnswer(),
                generated.getReferenceType(), perspectiveOf(category), null
        );
    }

    private QuestionSet assemble(QuestionSetCategory category, String questionText,
                                 String ttsText, String modelAnswer,
                                 String referenceType, FeedbackPerspective perspective,
                                 QuestionPool poolRef) {
        QuestionSet qs = QuestionSet.builder()
                .category(category)
                .orderIndex(0)
                .build();

        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText(questionText)
                .ttsText(ttsText)
                .modelAnswer(modelAnswer)
                .referenceType(parseReferenceType(referenceType))
                .feedbackPerspective(perspective)
                .orderIndex(0)
                .questionPool(poolRef)
                .build();

        qs.addQuestion(question);
        return qs;
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

    private ReferenceType parseReferenceType(String refTypeStr) {
        if (refTypeStr == null) {
            throw new IllegalArgumentException("referenceType은 null일 수 없습니다");
        }
        try {
            return ReferenceType.valueOf(refTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown referenceType: " + refTypeStr, e);
        }
    }

    private FeedbackPerspective perspectiveOf(InterviewType type) {
        return switch (type) {
            case BEHAVIORAL -> FeedbackPerspective.BEHAVIORAL;
            case RESUME_BASED -> FeedbackPerspective.EXPERIENCE;
            default -> FeedbackPerspective.TECHNICAL;
        };
    }

    private FeedbackPerspective perspectiveOf(QuestionSetCategory category) {
        return switch (category) {
            case BEHAVIORAL -> FeedbackPerspective.BEHAVIORAL;
            case RESUME_BASED -> FeedbackPerspective.EXPERIENCE;
            default -> FeedbackPerspective.TECHNICAL;
        };
    }
}
