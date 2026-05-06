package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.config.ContextEngineeringProperties;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.compaction.DialogueCompactor;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("DialogueHistoryLayer — L3 슬라이딩 윈도우 + compaction + raw fallback")
class DialogueHistoryLayerTest {

    @Mock
    private DialogueCompactor dialogueCompactor;

    private DialogueHistoryLayer layer;

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
        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash", CandidateLevel.MID, "backend", List.of(), null);
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
    @DisplayName("does_not_enter_compaction_when_within_window")
    void does_not_enter_compaction_when_within_window() {
        List<FollowUpExchange> exs = exchanges(5); // recentWindow == 5, no older turns
        ContextBuildRequest req = requestWith(exs, null);

        layer.build(req);

        verify(dialogueCompactor, never()).compactAsync(any(), anyInt(), any(), any());
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

    @Test
    @DisplayName("outputs_non_cached_messages")
    void outputs_non_cached_messages() {
        List<FollowUpExchange> exs = exchanges(3);
        ContextBuildRequest req = requestWith(exs, null);

        List<ChatMessage> result = layer.build(req);

        assertThat(result).allMatch(msg -> !msg.cacheControl());
    }

    @Nested
    @DisplayName("L3 raw fallback (P1-1)")
    class RawFallback {

        @Test
        @DisplayName("runtimeState 가 null 이고 window 를 초과하면 olderTurns 를 raw 로 포함하고 compactor 는 호출하지 않는다")
        void includes_raw_older_turns_when_runtime_state_null() {
            List<FollowUpExchange> exs = exchanges(7); // windowEnd = 2 (older = turns 1..2, recent = 3..7)
            ContextBuildRequest req = requestWith(exs, null);

            List<ChatMessage> result = layer.build(req);

            // older 2 turns × 2 + recent 5 turns × 2 = 14
            assertThat(result).hasSize(14);
            assertThat(result.get(0).role()).isEqualTo(ChatMessage.Role.USER);
            assertThat(result.get(0).content()).isEqualTo("Question 1");
            assertThat(result.get(3).content()).isEqualTo("Answer 2");
            assertThat(result.get(4).content()).isEqualTo("Question 3");
            verify(dialogueCompactor, never()).compactAsync(any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("요약 부재 + 압축 in-flight 이면 olderTurns 를 raw 로 포함하고 compactor 는 다시 호출하지 않는다")
        void includes_raw_older_turns_when_compaction_in_flight() {
            List<FollowUpExchange> exs = exchanges(7); // windowEnd = 2
            InterviewRuntimeState state = freshState();
            state.markCompactionStarted(2);
            ContextBuildRequest req = requestWith(exs, state);

            List<ChatMessage> result = layer.build(req);

            // older 2 turns + recent 5 turns alternating = 14
            assertThat(result).hasSize(14);
            assertThat(result.get(0).content()).isEqualTo("Question 1");
            assertThat(result.get(4).content()).isEqualTo("Question 3");
            verify(dialogueCompactor, never()).compactAsync(any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("요약 부재 + 압축 미시작 이면 olderTurns raw 포함과 동시에 compactor 를 호출한다")
        void includes_raw_and_triggers_compaction_when_summary_absent() {
            List<FollowUpExchange> exs = exchanges(7); // windowEnd = 2
            InterviewRuntimeState state = freshState();
            ContextBuildRequest req = requestWith(exs, state);

            List<ChatMessage> result = layer.build(req);

            assertThat(result).hasSize(14);
            assertThat(result.get(0).content()).isEqualTo("Question 1");
            verify(dialogueCompactor).compactAsync(eq(42L), eq(2), any(), eq(state));
        }
    }
}
