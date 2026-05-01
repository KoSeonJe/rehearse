package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetAnalysisRepository analysisRepository;
    private final QuestionSetFeedbackPersister feedbackPersister;
    private final ApplicationEventPublisher eventPublisher;

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void saveFeedback(Long questionSetId, SaveFeedbackRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);

        feedbackPersister.persist(questionSet, request);

        // plan-09에서 도메인 이벤트로 전환 예정. 현재는 직접 상태 전이
        analysis.completeAnalysis(request.isVerbalCompleted(), request.isNonverbalCompleted());

        log.info("분석 결과 저장 완료: questionSetId={}, verbal={}, nonverbal={}",
                questionSetId, request.isVerbalCompleted(), request.isNonverbalCompleted());

        Long interviewId = questionSet.getInterview().getId();
        if (isAllAnalysisSettled(interviewId)) {
            eventPublisher.publishEvent(new DeliveryEnrichmentRequestedEvent(interviewId));
            log.info("모든 QuestionSet 분석 완료 — DeliveryEnrichmentRequestedEvent 발행: interviewId={}", interviewId);
        }
    }

    private boolean isAllAnalysisSettled(Long interviewId) {
        long totalCount = questionSetRepository.countByInterviewId(interviewId);
        if (totalCount == 0) {
            return false;
        }
        long pendingCount = analysisRepository.countByQuestionSetInterviewIdAndAnalysisStatusIn(
                interviewId, AnalysisStatus.inProgressStatuses());
        long notStartedCount = analysisRepository.countByQuestionSetInterviewIdAndAnalysisStatusIn(
                interviewId, java.util.List.of(AnalysisStatus.PENDING, AnalysisStatus.PENDING_UPLOAD));
        return (pendingCount + notStartedCount) == 0;
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
