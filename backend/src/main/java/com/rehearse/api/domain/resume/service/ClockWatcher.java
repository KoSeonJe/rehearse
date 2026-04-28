package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 세션 경과 시간을 계산하는 stateless 유틸 컴포넌트.
 * InterviewRuntimeState.startedAt 을 기준으로 남은 시간을 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClockWatcher {

    private final InterviewRuntimeStateCache runtimeStateStore;
    private final Clock clock;

    public void markStart(Long interviewId) {
        runtimeStateStore.update(interviewId, state -> {
            if (state.getStartedAt() == null) {
                state.setStartedAt(Instant.now(clock));
                log.info("[ClockWatcher] 세션 시작 시각 기록: interviewId={}", interviewId);
            }
        });
    }

    public long remainingMinutes(Long interviewId, int durationMinutes) {
        InterviewRuntimeState state = runtimeStateStore.get(interviewId);
        Instant startedAt = state.getStartedAt();
        if (startedAt == null) {
            return durationMinutes;
        }
        long elapsedMinutes = ChronoUnit.MINUTES.between(startedAt, Instant.now(clock));
        long remaining = durationMinutes - elapsedMinutes;
        return Math.max(remaining, 0L);
    }
}
