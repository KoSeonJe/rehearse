package com.rehearse.api.infra.ai.context;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.global.config.ContextEngineeringProperties;
import com.rehearse.api.infra.ai.context.layer.DialogueHistoryLayer;
import com.rehearse.api.infra.ai.context.layer.FixedContextLayer;
import com.rehearse.api.infra.ai.context.layer.FocusLayer;
import com.rehearse.api.infra.ai.context.layer.SessionStateLayer;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewContextBuilder - 4-layer 컨텍스트 조립")
class InterviewContextBuilderTest {

    @Mock
    private FixedContextLayer l1;
    @Mock
    private SessionStateLayer l2;
    @Mock
    private DialogueHistoryLayer l3;
    @Mock
    private FocusLayer l4;

    private TokenEstimator tokenEstimator;

    private static final ChatMessage L1_MSG = ChatMessage.ofCached(ChatMessage.Role.SYSTEM, "l1 fixed content here for testing purposes");
    private static final ChatMessage L2_MSG = ChatMessage.of(ChatMessage.Role.SYSTEM, "l2 session state content");
    private static final ChatMessage L3_MSG_Q = ChatMessage.of(ChatMessage.Role.USER, "previous question");
    private static final ChatMessage L3_MSG_A = ChatMessage.of(ChatMessage.Role.ASSISTANT, "previous answer");
    private static final ChatMessage L4_MSG = ChatMessage.of(ChatMessage.Role.USER, "l4 focus user message");

    @BeforeEach
    void setUp() {
        tokenEstimator = new TokenEstimator();
    }

    private InterviewContextBuilder builderWith(boolean l4Enabled, int maxTokens) {
        ContextEngineeringProperties props = new ContextEngineeringProperties(true, 5, 5, l4Enabled, maxTokens);
        return new InterviewContextBuilder(l1, l2, l3, l4, tokenEstimator, props,
                new ContextEngineeringMetrics(new SimpleMeterRegistry()));
    }

    private ContextBuildRequest minimalRequest(String callType) {
        return new ContextBuildRequest(callType, Map.of(), List.of(), Map.of(), null);
    }

    @Test
    @DisplayName("assembles_l1_l2_l3_l4_in_order_when_full_session")
    void assembles_l1_l2_l3_l4_in_order_when_full_session() {
        given(l1.build(any())).willReturn(List.of(L1_MSG));
        given(l2.build(any())).willReturn(List.of(L2_MSG));
        given(l3.build(any())).willReturn(List.of(L3_MSG_Q, L3_MSG_A));
        given(l4.build(any())).willReturn(List.of(L4_MSG));

        InterviewContextBuilder builder = builderWith(true, 8000);
        BuiltContext result = builder.build(minimalRequest("answer_analyzer"));

        assertThat(result.messages()).hasSize(5);
        assertThat(result.messages().get(0)).isEqualTo(L1_MSG);
        assertThat(result.messages().get(1)).isEqualTo(L2_MSG);
        assertThat(result.messages().get(2)).isEqualTo(L3_MSG_Q);
        assertThat(result.messages().get(3)).isEqualTo(L3_MSG_A);
        assertThat(result.messages().get(4)).isEqualTo(L4_MSG);
    }

    @Test
    @DisplayName("omits_l4_when_disabled_in_properties")
    void omits_l4_when_disabled_in_properties() {
        given(l1.build(any())).willReturn(List.of(L1_MSG));
        given(l2.build(any())).willReturn(List.of(L2_MSG));
        given(l3.build(any())).willReturn(List.of());

        InterviewContextBuilder builder = builderWith(false, 8000);
        BuiltContext result = builder.build(minimalRequest("intent_classifier"));

        assertThat(result.messages()).hasSize(2);
        assertThat(result.messages()).doesNotContain(L4_MSG);
        verify(l4, never()).build(any());
    }

    @Test
    @DisplayName("total_tokens_below_8000_for_typical_10_turn_session")
    void total_tokens_below_8000_for_typical_10_turn_session() {
        // Build a realistic 10-turn exchange fixture (~3000 chars total for L3)
        List<FollowUpExchange> exchanges = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            exchanges.add(new FollowUpExchange(
                    "질문입니다. 자세히 설명해 주세요. 번호: " + i,
                    "답변입니다. 해당 개념은 이러이러합니다. 번호: " + i
            ));
        }

        // L1: ~1200 chars = ~300 tokens
        String l1Content = "a".repeat(1200);
        // L2: ~400 chars = ~100 tokens
        String l2Content = "b".repeat(400);
        // L3: 10 turns × ~100 chars = ~1000 chars = ~250 tokens
        List<ChatMessage> l3Messages = new ArrayList<>();
        for (FollowUpExchange ex : exchanges) {
            l3Messages.add(ChatMessage.of(ChatMessage.Role.USER, ex.getQuestion()));
            l3Messages.add(ChatMessage.of(ChatMessage.Role.ASSISTANT, ex.getAnswer()));
        }
        // L4: ~800 chars = ~200 tokens
        String l4Content = "c".repeat(800);

        given(l1.build(any())).willReturn(List.of(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, l1Content)));
        given(l2.build(any())).willReturn(List.of(ChatMessage.of(ChatMessage.Role.SYSTEM, l2Content)));
        given(l3.build(any())).willReturn(l3Messages);
        given(l4.build(any())).willReturn(List.of(ChatMessage.of(ChatMessage.Role.USER, l4Content)));

        InterviewContextBuilder builder = builderWith(true, 8000);
        ContextBuildRequest req = new ContextBuildRequest("follow_up_generator_v3", Map.of(), exchanges, Map.of(), null);
        BuiltContext result = builder.build(req);

        assertThat(result.tokenEstimate()).isLessThanOrEqualTo(8000);
    }

    @Test
    @DisplayName("per_layer_token_breakdown_present_in_built_context")
    void per_layer_token_breakdown_present_in_built_context() {
        given(l1.build(any())).willReturn(List.of(L1_MSG));
        given(l2.build(any())).willReturn(List.of(L2_MSG));
        given(l3.build(any())).willReturn(List.of());
        given(l4.build(any())).willReturn(List.of(L4_MSG));

        InterviewContextBuilder builder = builderWith(true, 8000);
        BuiltContext result = builder.build(minimalRequest("answer_analyzer"));

        Map<String, Integer> perLayer = result.perLayerTokens();
        assertThat(perLayer).containsKeys("L1", "L2", "L3", "L4", "total");
        assertThat(perLayer.get("L3")).isEqualTo(0);
        assertThat(perLayer.get("total")).isEqualTo(
                perLayer.get("L1") + perLayer.get("L2") + perLayer.get("L3") + perLayer.get("L4"));
    }

    @Test
    @DisplayName("logs_warn_when_total_exceeds_max_no_exception_thrown")
    void logs_warn_when_total_exceeds_max_no_exception_thrown() {
        // L1 alone has 10000 chars → ~2500 tokens, max is 100
        String hugeContent = "x".repeat(10000);
        given(l1.build(any())).willReturn(List.of(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, hugeContent)));
        given(l2.build(any())).willReturn(List.of());
        given(l3.build(any())).willReturn(List.of());
        given(l4.build(any())).willReturn(List.of());

        // maxContextTokens=100 to force the warn path; l4JustInTime=true but L4 returns empty
        ContextEngineeringProperties props = new ContextEngineeringProperties(true, 5, 5, true, 2000);
        InterviewContextBuilder builder = new InterviewContextBuilder(l1, l2, l3, l4, tokenEstimator, props,
                new ContextEngineeringMetrics(new SimpleMeterRegistry()));

        // Should NOT throw — just log warn
        BuiltContext result = builder.build(minimalRequest("answer_analyzer"));
        assertThat(result.tokenEstimate()).isGreaterThan(2000);
    }
}
