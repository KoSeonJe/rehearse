package com.rehearse.api.domain.questionset.entity;

public enum AnalysisStatus {
    PENDING,
    PENDING_UPLOAD,
    EXTRACTING,
    ANALYZING,
    FINALIZING,
    COMPLETED,
    PARTIAL,
    FAILED,
    SKIPPED;

    public boolean canTransitionTo(AnalysisStatus target) {
        return switch (this) {
            case PENDING -> target == PENDING_UPLOAD || target == SKIPPED || target == FAILED;
            case PENDING_UPLOAD -> target == EXTRACTING || target == FAILED;
            case EXTRACTING -> target == ANALYZING || target == FAILED;
            case ANALYZING -> target == FINALIZING || target == FAILED;
            case FINALIZING -> target == COMPLETED || target == PARTIAL || target == FAILED;
            case COMPLETED -> target == FAILED;
            case PARTIAL -> target == EXTRACTING || target == FAILED;
            case FAILED -> target == EXTRACTING;
            case SKIPPED -> false;
        };
    }

    /**
     * 분석이 더 이상 필요 없는 상태 (완료, 부분 완료, 또는 건너뜀).
     * FAILED는 재시도 가능하므로 포함하지 않음.
     */
    public boolean isResolved() {
        return this == COMPLETED || this == PARTIAL || this == SKIPPED;
    }
}
