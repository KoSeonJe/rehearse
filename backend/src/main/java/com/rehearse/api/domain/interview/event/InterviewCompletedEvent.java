package com.rehearse.api.domain.interview.event;

import java.time.LocalDateTime;

public record InterviewCompletedEvent(Long interviewId, LocalDateTime completedAt) {
}
