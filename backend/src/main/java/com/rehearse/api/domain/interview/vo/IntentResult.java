package com.rehearse.api.domain.interview.vo;

public record IntentResult(IntentType type, double confidence, String reasoning, boolean fallback) {

    public static IntentResult forceAnswer() {
        return new IntentResult(IntentType.ANSWER, 0.0, "low confidence or error fallback", true);
    }

    public static IntentResult of(IntentType type, double confidence, String reasoning) {
        return new IntentResult(type, confidence, reasoning, false);
    }
}
