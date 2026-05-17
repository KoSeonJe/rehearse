package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.rubric.service.RubricBackfillScorer;
import com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.interview.event.InterviewCompletedEvent;
import com.rehearse.api.global.config.SessionFeedbackExecutorConfig;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackEventListener {

    private final SessionFeedbackService sessionFeedbackService;
    private final RubricBackfillScorer rubricBackfillScorer;
    private final AiCallMetrics aiCallMetrics;

    @Async(SessionFeedbackExecutorConfig.SESSION_FEEDBACK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterviewCompletedEvent event) {
        Long interviewId = event.interviewId();
        try {
            try {
                rubricBackfillScorer.backfill(interviewId);
            } catch (Exception backfillEx) {
                log.warn("RubricBackfill 실패 — synthesis 는 계속 진행: interviewId={}", interviewId, backfillEx);
                aiCallMetrics.incrementRubricFailure("backfill_listener_failed");
            }
            sessionFeedbackService.synthesizePreliminary(interviewId);
        } catch (SessionFeedbackParseException e) {
            sessionFeedbackService.recordSynthesisFailure(interviewId, "PARSE_FAILED");
            aiCallMetrics.incrementSynthesizerFailure("PARSE_FAILED");
        } catch (Exception e) {
            sessionFeedbackService.recordSynthesisFailure(interviewId, "INTERNAL_ERROR");
            aiCallMetrics.incrementSynthesizerFailure("INTERNAL_ERROR");
            log.warn("SessionFeedback synthesize 실패: interviewId={}", interviewId, e);
        }
    }

    @Async(SessionFeedbackExecutorConfig.SESSION_FEEDBACK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DeliveryEnrichmentRequestedEvent event) {
        Long interviewId = event.interviewId();
        try {
            sessionFeedbackService.enrichDeliveryFromScores(interviewId);
        } catch (SessionFeedbackParseException e) {
            sessionFeedbackService.recordSynthesisFailure(interviewId, "PARSE_FAILED");
            aiCallMetrics.incrementSynthesizerFailure("PARSE_FAILED");
            log.warn("SessionFeedback enrichDelivery parse 실패: interviewId={}", interviewId, e);
        } catch (Exception e) {
            sessionFeedbackService.recordSynthesisFailure(interviewId, "INTERNAL_ERROR");
            aiCallMetrics.incrementSynthesizerFailure("INTERNAL_ERROR");
            log.warn("SessionFeedback enrichDelivery 실패: interviewId={}", interviewId, e);
        }
    }
}
