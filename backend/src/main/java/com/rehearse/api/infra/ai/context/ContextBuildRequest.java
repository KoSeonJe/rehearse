package com.rehearse.api.infra.ai.context;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;

import java.util.List;
import java.util.Map;

public record ContextBuildRequest(
        String callType,
        Map<String, Object> runtimeState,
        List<FollowUpExchange> exchanges,
        Map<String, Object> focusHints,
        String providerHint
) {
    public ContextBuildRequest {
        if (callType == null || callType.isBlank()) {
            throw new IllegalArgumentException("callType must not be blank");
        }
        if (runtimeState == null) {
            runtimeState = Map.of();
        }
        if (exchanges == null) {
            exchanges = List.of();
        }
        if (focusHints == null) {
            focusHints = Map.of();
        }
    }
}
