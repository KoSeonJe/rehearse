package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.dto.ChatMessage;

import java.util.List;

public interface ContextLayer {
    List<ChatMessage> build(ContextBuildRequest req);
}
