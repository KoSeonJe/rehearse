package com.rehearse.api.infra.ai.context.layer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.CandidateLevel;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;

class SessionStateLayerTest {

    private SessionStateLayer layer;
    private TokenEstimator tokenEstimator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tokenEstimator = new TokenEstimator();
        layer = new SessionStateLayer(objectMapper, tokenEstimator);
    }

    private ContextBuildRequest requestWith(InterviewRuntimeState state) {
        Map<String, Object> runtimeState = state == null
                ? Map.of()
                : Map.of(SessionStateLayer.RUNTIME_STATE_KEY, state);
        return new ContextBuildRequest("answer_analyzer", runtimeState, null, null, null);
    }

    private InterviewRuntimeState freshState(String level) {
        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash", CandidateLevel.MID, "backend", List.of(), null);
        return new InterviewRuntimeState(level, skeleton);
    }

    @Test
    @DisplayName("returns_empty_list_when_runtime_state_is_null")
    void returns_empty_list_when_runtime_state_is_null() {
        ContextBuildRequest req = requestWith(null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("serializes_snapshot_with_5_fields_when_state_present")
    void serializes_snapshot_with_5_fields_when_state_present() {
        InterviewRuntimeState state = freshState("MID");
        state.getPlaygroundTurns().set(4);
        state.addCoveredClaim("트레이드오프 언급");
        state.getActiveChain().add(101L);
        state.getActiveChain().add(102L);

        List<ChatMessage> result = layer.build(requestWith(state));

        assertThat(result).hasSize(1);
        String content = result.get(0).content();
        assertThat(content).contains("\"level\"");
        assertThat(content).contains("\"current_turn\"");
        assertThat(content).contains("\"covered_claims_recent\"");
        assertThat(content).contains("\"active_chain\"");
        assertThat(content).contains("\"asked_perspectives\"");
    }

    @Test
    @DisplayName("trims_covered_claims_to_recent_50_when_more_than_100_present")
    void trims_covered_claims_to_recent_50_when_more_than_100_present() throws Exception {
        InterviewRuntimeState state = freshState("SENIOR");
        for (int i = 0; i < 120; i++) {
            state.addCoveredClaim("claim-" + i);
        }

        List<ChatMessage> result = layer.build(requestWith(state));

        assertThat(result).hasSize(1);
        String json = result.get(0).content().replace("## SESSION STATE\n", "");
        var node = objectMapper.readTree(json);
        var claims = node.get("covered_claims_recent");
        assertThat(claims.size()).isLessThanOrEqualTo(50);
        // most-recent claims (70–119) are kept; earliest claims (0–69) are dropped
        assertThat(claims.get(claims.size() - 1).asText()).isEqualTo("claim-119");
        assertThat(claims.get(0).asText()).isEqualTo("claim-70");
    }

    @Test
    @DisplayName("enforces_500_token_cap_when_state_oversized")
    void enforces_500_token_cap_when_state_oversized() {
        InterviewRuntimeState state = freshState("MID");
        // Add enough claims to push token count well above 500
        for (int i = 0; i < 200; i++) {
            state.addCoveredClaim("this-is-a-fairly-long-claim-text-that-takes-up-tokens-number-" + i);
        }

        List<ChatMessage> result = layer.build(requestWith(state));

        assertThat(result).hasSize(1);
        int tokens = tokenEstimator.estimate(result);
        assertThat(tokens).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("produces_non_cached_system_message")
    void produces_non_cached_system_message() {
        InterviewRuntimeState state = freshState("JUNIOR");

        List<ChatMessage> result = layer.build(requestWith(state));

        assertThat(result).hasSize(1);
        ChatMessage msg = result.get(0);
        assertThat(msg.role()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(msg.cacheControl()).isFalse();
    }

    @Test
    @DisplayName("serializes_snake_case_keys")
    void serializes_snake_case_keys() {
        InterviewRuntimeState state = freshState("MID");

        List<ChatMessage> result = layer.build(requestWith(state));

        assertThat(result).hasSize(1);
        String content = result.get(0).content();
        assertThat(content).contains("current_turn");
        assertThat(content).contains("covered_claims_recent");
        assertThat(content).contains("active_chain");
        assertThat(content).contains("asked_perspectives");
        // camelCase must not appear
        assertThat(content).doesNotContain("currentTurn");
        assertThat(content).doesNotContain("coveredClaimsRecent");
        assertThat(content).doesNotContain("activeChain");
        assertThat(content).doesNotContain("askedPerspectives");
    }
}
