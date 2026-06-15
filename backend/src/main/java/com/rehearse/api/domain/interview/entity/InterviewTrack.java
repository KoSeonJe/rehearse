package com.rehearse.api.domain.interview.entity;

public enum InterviewTrack {
    CS,
    LANGUAGE,
    RESUME;

    public String logLabel() {
        return this == RESUME ? "RESUME" : "STANDARD";
    }
}
