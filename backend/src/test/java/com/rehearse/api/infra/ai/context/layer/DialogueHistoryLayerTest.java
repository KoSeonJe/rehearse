package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.CachedResumeSkeleton;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.global.config.ContextEngineeringProperties;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.compaction.DialogueCompactor;
import com.rehearse.api.infra.ai.dto.ChatMessage;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DialogueHistoryLayer — L3 슬라이딩 윈도우 + compaction")
class DialogueHistoryLayerTest {

    @Mock
    private DialogueCompactor dialogueCompactor;

    private DialogueHistoryLayer layer;

    // l3RecentWindow=5, l3CompactionThreshold=5 matches original RECENT_WINDOW=5 behaviour
    private static final ContextEngineeringProperties PROPS =
            new ContextEngineeringProperties(true, 5, 5, true, 8000);

    @BeforeEach
    void setUp() {
        layer = new DialogueHistoryLayer(dialogueCompactor, PROPS);
    }

    private FollowUpExchange exchange(int n) {
        return new FollowUpExchange("Question " + n, "Answer " + n);
    }

    private List<FollowUpExchange> exchanges(int count) {
        List<FollowUpExchange> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(exchange(i));
        }
        return list;
    }

    private InterviewRuntimeState freshState() {
        CachedResumeSkeleton skeleton = () -> "hash";
        return new InterviewRuntimeState("MID", skeleton);
    }

    private ContextBuildRequest requestWith(List<FollowUpExchange> exs, InterviewRuntimeState state) {
        Map<String, Object> runtimeStateMap = state == null
                ? Map.of()
                : Map.of(
                        DialogueHistoryLayer.RUNTIME_STATE_KEY, state,
                        DialogueHistoryLayer.INTERVIEW_ID_KEY, 42L
                );
        return new ContextBuildRequest("follow_up_generator_v3", runtimeStateMap, exs, null, null);
    }

    @Test
    @DisplayName("returns_empty_when_no_exchanges")
    void returns_empty_when_no_exchanges() {
        ContextBuildRequest req = requestWith(List.of(), null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("renders_alternating_messages_when_within_window")
    void renders_alternating_messages_when_within_window() {
        List<FollowUpExchange> exs = exchanges(3);
        ContextBuildRequest req = requestWith(exs, null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).hasSize(6); // 3 USER + 3 ASSISTANT alternating
        assertThat(result.get(0).role()).isEqualTo(ChatMessage.Role.USER);
        assertThat(result.get(0).content()).isEqualTo("Question 1");
        assertThat(result.get(1).role()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(result.get(1).content()).isEqualTo("Answer 1");
        assertThat(result.get(4).role()).isEqualTo(ChatMessage.Role.USER);
        assertThat(result.get(4).content()).isEqualTo("Question 3");
    }

    @Test
    @DisplayName("triggers_compaction_when_over_window_and_no_cached_summary")
    void triggers_compaction_when_over_window_and_no_cached_summary() {
        List<FollowUpExchange> exs = exchanges(7); // windowEnd = 7 - 5 = 2
        InterviewRuntimeState state = freshState();
        ContextBuildRequest req = requestWith(exs, state);

        layer.build(req);

        verify(dialogueCompactor).compactAsync(eq(42L), eq(2), any(), eq(state));
    }

    @Test
    @DisplayName("prepends_cached_summary_when_present")
    void prepends_cached_summary_when_present() {
        List<FollowUpExchange> exs = exchanges(7); // windowEnd = 2
        InterviewRuntimeState state = freshState();
        state.putCompactedSummary(2, "[topics] GC / [claims] STW 발생");
        ContextBuildRequest req = requestWith(exs, state);

        List<ChatMessage> result = layer.build(req);

        // First message is the summary, then 5 recent turns × 2 messages each
        assertThat(result).hasSize(11);
        ChatMessage summaryMsg = result.get(0);
        assertThat(summaryMsg.role()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(summaryMsg.content()).contains("DIALOGUE SUMMARY");
        assertThat(summaryMsg.content()).contains("turns 1..2");
        assertThat(summaryMsg.content()).contains("GC");
    }

    @Test
    @DisplayName("omits_summary_when_compaction_in_flight_first_turn")
    void omits_summary_when_compaction_in_flight_first_turn() {
        List<FollowUpExchange> exs = exchanges(7); // windowEnd = 2
        InterviewRuntimeState state = freshState();
        state.markCompactionStarted(2); // already in-flight
        ContextBuildRequest req = requestWith(exs, state);

        List<ChatMessage> result = layer.build(req);

        // No summary — just recent 5 turns
        assertThat(result).hasSize(10);
        assertThat(result.get(0).role()).isEqualTo(ChatMessage.Role.USER);
        // Compactor must NOT be called again (already in-flight)
        verify(dialogueCompactor, never()).compactAsync(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("outputs_non_cached_messages")
    void outputs_non_cached_messages() {
        List<FollowUpExchange> exs = exchanges(3);
        ContextBuildRequest req = requestWith(exs, null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).allMatch(msg -> !msg.cacheControl());
    }

    @Test
    @DisplayName("recent_window_contains_last_5_turns_when_over_window")
    void recent_window_contains_last_5_turns_when_over_window() {
        List<FollowUpExchange> exs = exchanges(8); // windowEnd = 3, recent = turns 4..8
        InterviewRuntimeState state = freshState();
        state.putCompactedSummary(3, "some summary");
        ContextBuildRequest req = requestWith(exs, state);

        List<ChatMessage> result = layer.build(req);

        // 1 summary + 5 recent × 2 = 11
        assertThat(result).hasSize(11);
        // Second message (index 1) is first USER of recent window = turn 4
        assertThat(result.get(1).content()).isEqualTo("Question 4");
        // Last message is ASSISTANT of turn 8
        assertThat(result.get(10).content()).isEqualTo("Answer 8");
    }
}
