package com.rehearse.api.infra.ai.context;

import com.rehearse.api.infra.ai.dto.ChatMessage;

import java.util.List;
import java.util.Map;

public record BuiltContext(
        List<ChatMessage> messages,
        int tokenEstimate,
        Map<String, Integer> perLayerTokens
) {
    public BuiltContext {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("BuiltContext.messages must not be empty");
        }
        if (perLayerTokens == null) {
            perLayerTokens = Map.of();
        }
    }
}
