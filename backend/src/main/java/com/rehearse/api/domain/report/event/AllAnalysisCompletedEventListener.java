package com.rehearse.api.domain.report.event;

import com.rehearse.api.domain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllAnalysisCompletedEventListener {

    private final ReportService reportService;

    @Async
    @TransactionalEventListener
    public void handleAllAnalysisCompleted(AllAnalysisCompletedEvent event) {
        try {
            log.info("AllAnalysisCompletedEvent 수신 → 리포트 자동 생성: interviewId={}", event.getInterviewId());
            reportService.generateReport(event.getInterviewId());
        } catch (Exception e) {
            log.error("리포트 자동 생성 실패: interviewId={}", event.getInterviewId(), e);
        }
    }
}
