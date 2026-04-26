package com.rehearse.api.infra.ai.context;

import com.rehearse.api.global.config.ContextEngineeringProperties;
import com.rehearse.api.infra.ai.context.layer.DialogueHistoryLayer;
import com.rehearse.api.infra.ai.context.layer.FixedContextLayer;
import com.rehearse.api.infra.ai.context.layer.FocusLayer;
import com.rehearse.api.infra.ai.context.layer.SessionStateLayer;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewContextBuilder {

    private final FixedContextLayer l1;
    private final SessionStateLayer l2;
    private final DialogueHistoryLayer l3;
    private final FocusLayer l4;
    private final TokenEstimator tokenEstimator;
    private final ContextEngineeringProperties properties;
    private final ContextEngineeringMetrics contextMetrics;

    public BuiltContext build(ContextBuildRequest req) {
        var messages = new ArrayList<ChatMessage>();
        var perLayer = new LinkedHashMap<String, Integer>();

        addLayer(messages, perLayer, "L1", l1.build(req));
        addLayer(messages, perLayer, "L2", l2.build(req));
        addLayer(messages, perLayer, "L3", l3.build(req));
        if (properties.l4JustInTime()) {
            addLayer(messages, perLayer, "L4", l4.build(req));
        }

        int total = perLayer.values().stream().mapToInt(Integer::intValue).sum();
        perLayer.put("total", total);

        if (total > properties.maxContextTokens()) {
            log.warn("[ContextBuilder] token budget exceeded: callType={}, total={}, max={}",
                    req.callType(), total, properties.maxContextTokens());
        }

        BuiltContext built = new BuiltContext(List.copyOf(messages), total, Map.copyOf(perLayer));
        contextMetrics.recordContextTokens(req.callType(), built);
        return built;
    }

    private void addLayer(List<ChatMessage> messages, Map<String, Integer> perLayer,
                          String layerName, List<ChatMessage> layerMessages) {
        if (layerMessages.isEmpty()) {
            perLayer.put(layerName, 0);
            return;
        }
        messages.addAll(layerMessages);
        perLayer.put(layerName, tokenEstimator.estimate(layerMessages));
    }
}
