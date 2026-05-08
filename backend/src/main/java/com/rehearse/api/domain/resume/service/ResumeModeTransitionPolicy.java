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

    @Value("${rehearse.resume-track.playground-max-turns:3}")
    private int playgroundMaxTurns;

    public boolean isHardTimeoutExceeded(int durationMinutes, long remainingMinutes) {
        long elapsedMinutes = durationMinutes - remainingMinutes;
        return elapsedMinutes >= durationMinutes + hardTimeoutMin;
    }

    public boolean isPlaygroundHardCapReached(int turnCount) {
        return turnCount >= playgroundMaxTurns;
    }

    public int getPlaygroundMaxTurns() {
        return playgroundMaxTurns;
    }
}
