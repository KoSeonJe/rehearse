package com.rehearse.api.domain.questionset.entity;

public enum AnalysisProgress {
    STARTED,
    EXTRACTING,
    STT_PROCESSING,
    VERBAL_ANALYZING,
    NONVERBAL_ANALYZING,
    ANALYZING,             // Gemini + Vision 병렬 단계
    FINALIZING,
    FAILED
}
