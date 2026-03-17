package com.rehearse.api.domain.questionset.entity;

public enum AnalysisStatus {
    PENDING,
    PENDING_UPLOAD,
    ANALYZING,
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(AnalysisStatus target) {
        return switch (this) {
            case PENDING -> target == PENDING_UPLOAD || target == FAILED;
            case PENDING_UPLOAD -> target == ANALYZING || target == FAILED;
            case ANALYZING -> target == COMPLETED || target == FAILED;
            case COMPLETED -> target == FAILED;
            case FAILED -> target == PENDING_UPLOAD;
        };
    }
}
