package com.devlens.api.domain.interview.entity;

public enum InterviewStatus {
    READY,
    IN_PROGRESS,
    COMPLETED;

    public boolean canTransitionTo(InterviewStatus target) {
        return switch (this) {
            case READY -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == COMPLETED;
            case COMPLETED -> false;
        };
    }
}
