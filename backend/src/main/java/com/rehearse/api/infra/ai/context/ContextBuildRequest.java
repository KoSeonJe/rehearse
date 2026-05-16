package com.rehearse.api.infra.ai.context;

public record ContextBuildRequest(
        String callType,
        FocusHints focusHints,
        String providerHint
) {
    public ContextBuildRequest {
        if (callType == null || callType.isBlank()) {
            throw new IllegalArgumentException("callType must not be blank");
        }
        if (focusHints == null) {
            focusHints = FocusHints.EmptyHints.INSTANCE;
        }
    }
}
