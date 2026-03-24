package com.rehearse.api.domain.questionset.entity;

public enum ConvertStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(ConvertStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING || target == FAILED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED -> false;
            case FAILED -> target == PROCESSING;
        };
    }
}
