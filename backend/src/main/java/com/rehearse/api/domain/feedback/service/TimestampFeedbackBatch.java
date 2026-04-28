package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.mapper.TimestampFeedbackMapper;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimestampFeedbackBatch {

    private final QuestionRepository questionRepository;
    private final TimestampFeedbackMapper timestampFeedbackMapper;

    public void attachTo(QuestionSetFeedback feedback, List<SaveFeedbackRequest.TimestampFeedbackItem> items) {
        if (items == null) {
            return;
        }
        for (SaveFeedbackRequest.TimestampFeedbackItem item : items) {
            Question question = resolveQuestion(item.getQuestionId());
            TimestampFeedback timestampFeedback = timestampFeedbackMapper.toEntity(item, question);
            feedback.addTimestampFeedback(timestampFeedback);
        }
    }

    private Question resolveQuestion(Long questionId) {
        if (questionId == null) {
            return null;
        }
        return questionRepository.findById(questionId)
                .orElseGet(() -> {
                    log.warn("피드백 저장 시 존재하지 않는 questionId={}", questionId);
                    return null;
                });
    }
}
