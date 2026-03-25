package com.rehearse.api.domain.questionset.entity;

import java.util.Arrays;
import java.util.List;

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
            case PENDING -> target == PENDING_UPLOAD || target == EXTRACTING || target == SKIPPED || target == FAILED;
            case PENDING_UPLOAD -> target == EXTRACTING || target == FAILED;
            case EXTRACTING -> target == ANALYZING || target == FAILED;
            case ANALYZING -> target == FINALIZING || target == FAILED;
            case FINALIZING -> target == COMPLETED || target == PARTIAL || target == FAILED;
            case COMPLETED -> target == FAILED;
            case PARTIAL -> target == EXTRACTING || target == COMPLETED || target == FAILED;
            case FAILED -> target == EXTRACTING || target == COMPLETED;
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

    /** 완전히 분석 완료된 상태 */
    public boolean isFullyCompleted() {
        return this == COMPLETED;
    }

    /** 부분 완료 상태 (일부 분석 성공, 일부 실패) */
    public boolean isPartiallyCompleted() {
        return this == PARTIAL;
    }

    /** 재시도 가능한 상태 (FAILED 또는 PARTIAL) */
    public boolean isRetryable() {
        return this == FAILED || this == PARTIAL;
    }

    /** 분석 결과(피드백)가 존재하는 상태 */
    public boolean hasAnalysisResult() {
        return this == COMPLETED || this == PARTIAL;
    }

    /** Lambda가 처리 중인 상태 (좀비 감지 대상) */
    public boolean isInProgress() {
        return this == EXTRACTING || this == ANALYZING || this == FINALIZING;
    }

    public static List<AnalysisStatus> inProgressStatuses() {
        return Arrays.stream(values()).filter(AnalysisStatus::isInProgress).toList();
    }
}
