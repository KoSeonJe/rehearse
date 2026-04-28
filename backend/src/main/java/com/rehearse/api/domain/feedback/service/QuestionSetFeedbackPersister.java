package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionSetFeedbackPersister {

    private final QuestionSetFeedbackRepository feedbackRepository;
    private final TimestampFeedbackBatch timestampFeedbackBatch;

    @Transactional
    public QuestionSetFeedback persist(QuestionSet questionSet, SaveFeedbackRequest request) {
        QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment(request.getQuestionSetComment())
                .build();

        timestampFeedbackBatch.attachTo(feedback, request.getTimestampFeedbacks());

        return feedbackRepository.save(feedback);
    }
}
