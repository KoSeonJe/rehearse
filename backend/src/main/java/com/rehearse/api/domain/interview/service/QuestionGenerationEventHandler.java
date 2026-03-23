package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionGenerationEventHandler {

    private final QuestionGenerationService questionGenerationService;

    @Async("questionGenerationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionGenerationEvent(QuestionGenerationRequestedEvent event) {
        try {
            questionGenerationService.generateQuestions(
                    event.getInterviewId(), event.getPosition(), event.getLevel(),
                    event.getInterviewTypes(), event.getCsSubTopics(),
                    event.getResumeText(), event.getDurationMinutes(), event.getTechStack());
        } catch (Exception e) {
            log.error("질문 생성 비동기 작업 실패: interviewId={}", event.getInterviewId(), e);
            String reason = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            questionGenerationService.failGeneration(event.getInterviewId(),
                    reason != null ? reason : "알 수 없는 오류");
        }
    }
}
