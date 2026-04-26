package com.rehearse.api.infra.ai.context.layer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.SessionStateSnapshot;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionStateLayer implements ContextLayer {

    static final String RUNTIME_STATE_KEY = "interviewRuntimeState";

    private static final int MAX_TOKENS = 500;
    private static final int INITIAL_CLAIM_TRIM = 50;
    private static final String HEADER = "## SESSION STATE\n";

    private final ObjectMapper objectMapper;
    private final TokenEstimator tokenEstimator;

    @Override
    public List<ChatMessage> build(ContextBuildRequest req) {
        InterviewRuntimeState runtimeState =
                (InterviewRuntimeState) req.runtimeState().get(RUNTIME_STATE_KEY);
        if (runtimeState == null) {
            return List.of();
        }

        SessionStateSnapshot snapshot = runtimeState.toSessionStateSnapshot();
        String json = serializeWithTrimming(snapshot, runtimeState);
        return List.of(ChatMessage.of(ChatMessage.Role.SYSTEM, HEADER + json));
    }

    private String serializeWithTrimming(SessionStateSnapshot snapshot, InterviewRuntimeState state) {
        String json = toJson(snapshot);
        if (tokenEstimator.estimate(HEADER + json) <= MAX_TOKENS) {
            return json;
        }

        // Binary halving of covered_claims_recent until within token cap
        List<String> claims = new ArrayList<>(snapshot.coveredClaimsRecent());
        int count = Math.min(claims.size(), INITIAL_CLAIM_TRIM);
        while (count > 0) {
            count = count / 2;
            List<String> trimmed = claims.subList(Math.max(0, claims.size() - count), claims.size());
            SessionStateSnapshot candidate = new SessionStateSnapshot(
                    snapshot.level(),
                    snapshot.currentTurn(),
                    List.copyOf(trimmed),
                    snapshot.activeChain(),
                    snapshot.askedPerspectives()
            );
            String candidate_json = toJson(candidate);
            if (tokenEstimator.estimate(HEADER + candidate_json) <= MAX_TOKENS) {
                return candidate_json;
            }
        }

        // count reached 0 — serialize with empty claims
        SessionStateSnapshot minimal = new SessionStateSnapshot(
                snapshot.level(),
                snapshot.currentTurn(),
                List.of(),
                snapshot.activeChain(),
                snapshot.askedPerspectives()
        );
        return toJson(minimal);
    }

    private String toJson(SessionStateSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("SessionStateSnapshot 직렬화 실패", e);
        }
    }
}
