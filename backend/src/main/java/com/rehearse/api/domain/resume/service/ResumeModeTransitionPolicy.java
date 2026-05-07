package com.rehearse.api.domain.resume.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeModeTransitionPolicy {

    @Value("${rehearse.resume-track.hard-timeout-min:10}")
    private long hardTimeoutMin;

    public boolean isHardTimeoutExceeded(int durationMinutes, long remainingMinutes) {
        long elapsedMinutes = durationMinutes - remainingMinutes;
        return elapsedMinutes >= durationMinutes + hardTimeoutMin;
    }
}
