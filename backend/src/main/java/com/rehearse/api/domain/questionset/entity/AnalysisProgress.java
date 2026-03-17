package com.rehearse.api.domain.questionset.entity;

public enum AnalysisProgress {
    STARTED,
    EXTRACTING,
    STT_PROCESSING,
    VERBAL_ANALYZING,
    NONVERBAL_ANALYZING,
    FINALIZING,
    FAILED
}
