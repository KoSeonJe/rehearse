package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final QuestionSetFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetAnalysisRepository analysisRepository;
    private final TimestampFeedbackMapper timestampFeedbackMapper;

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void saveFeedback(Long questionSetId, SaveFeedbackRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);

        QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment(request.getQuestionSetComment())
                .build();

        if (request.getTimestampFeedbacks() != null) {
            for (SaveFeedbackRequest.TimestampFeedbackItem item : request.getTimestampFeedbacks()) {
                Question question = null;
                if (item.getQuestionId() != null) {
                    question = questionRepository.findById(item.getQuestionId())
                            .orElseGet(() -> {
                                log.warn("피드백 저장 시 존재하지 않는 questionId={}", item.getQuestionId());
                                return null;
                            });
                }

                TimestampFeedback timestampFeedback = timestampFeedbackMapper.toEntity(item, question);
                feedback.addTimestampFeedback(timestampFeedback);
            }
        }

        feedbackRepository.save(feedback);
        analysis.completeAnalysis(
                request.isVerbalCompleted(),
                request.isNonverbalCompleted()
        );

        log.info("분석 결과 저장 완료: questionSetId={}, verbal={}, nonverbal={}",
                questionSetId, request.isVerbalCompleted(), request.isNonverbalCompleted());
    }

    private QuestionSet findQuestionSet(Long questionSetId) {
        return questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }

    private QuestionSetAnalysis findAnalysis(Long questionSetId) {
        return analysisRepository.findByQuestionSetId(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }
}
