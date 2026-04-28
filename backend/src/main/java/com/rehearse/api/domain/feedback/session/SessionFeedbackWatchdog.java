package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackWatchdog {

    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final SessionFeedbackService sessionFeedbackService;

    @Value("${rehearse.feedback-synthesizer.delivery-timeout-minutes:10}")
    private int deliveryTimeoutMinutes;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkStalePreliminaries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(deliveryTimeoutMinutes);

        sessionFeedbackRepository
                .findByStatusAndCreatedAtBefore(SessionFeedbackStatus.PRELIMINARY, cutoff)
                .forEach(feedback -> {
                    try {
                        sessionFeedbackService.markCompleteDueToTimeout(feedback.getInterviewId());
                    } catch (Exception e) {
                        log.warn("Watchdog timeout 처리 실패: interviewId={}, reason={}",
                                feedback.getInterviewId(), e.getMessage());
                    }
                });
    }
}
