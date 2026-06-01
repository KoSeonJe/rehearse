package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.event.DeliveryEnrichmentRequestedEvent;
import com.rehearse.api.domain.interview.event.InterviewCompletedEvent;
import com.rehearse.api.global.config.SessionFeedbackExecutorConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// PR1 중립화: 루브릭 점수 제거로 세션 피드백 입력 소스가 사라져 생성 경로를 비활성화한다.
// PR2(plan-548)에서 timestamp_feedback 코멘트 기반 입력으로 재설계 후 재활성 예정.
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackEventListener {

    @Async(SessionFeedbackExecutorConfig.SESSION_FEEDBACK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InterviewCompletedEvent event) {
        log.info("세션 피드백 생성 임시 비활성(PR1 중립화) — InterviewCompletedEvent skip: interviewId={}",
                event.interviewId());
    }

    @Async(SessionFeedbackExecutorConfig.SESSION_FEEDBACK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DeliveryEnrichmentRequestedEvent event) {
        log.info("세션 피드백 enrichment 임시 비활성(PR1 중립화) — DeliveryEnrichmentRequestedEvent skip: interviewId={}",
                event.interviewId());
    }
}
