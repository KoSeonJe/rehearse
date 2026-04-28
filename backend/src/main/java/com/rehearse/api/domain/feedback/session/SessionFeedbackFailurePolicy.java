package com.rehearse.api.domain.feedback.session;

public final class SessionFeedbackFailurePolicy {

    private SessionFeedbackFailurePolicy() {}

    public static boolean isRetryable(String reason) {
        // 현 spec: 모든 reason은 retryable (향후 일부 false 전환 가능)
        return true;
    }
}
