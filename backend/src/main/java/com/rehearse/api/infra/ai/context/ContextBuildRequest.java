package com.rehearse.api.infra.ai.context;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

public record ContextBuildRequest(
        String callType,
        FocusHints focusHints,
        String providerHint,
        Position position,
        TechStack techStack
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
