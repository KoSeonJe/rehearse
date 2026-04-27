package com.rehearse.api.infra.ai.context.compaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DialogueCompactor — 비동기 요약 및 상태 관리")
class DialogueCompactorTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    private DialogueCompactor compactor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        compactor = new DialogueCompactor(aiClient, aiResponseParser, objectMapper,
                new ContextEngineeringMetrics(new SimpleMeterRegistry()));
        // Inject pre-loaded system prompt to bypass @PostConstruct file loading
        ReflectionTestUtils.setField(compactor, "systemPromptTemplate", "system prompt for test");
    }

    private InterviewRuntimeState freshState() {
        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash", ResumeSkeleton.CandidateLevel.MID, "backend", List.of(), null);
        return new InterviewRuntimeState("MID", skeleton);
    }

    private List<FollowUpExchange> sampleExchanges() {
        return List.of(
                new FollowUpExchange("GC 방식은?", "Young/Old 세대 구조입니다"),
                new FollowUpExchange("STW 영향은?", "처리량 손실이 발생합니다")
        );
    }

    private ChatResponse stubResponse(String json) {
        return new ChatResponse(json, null, "openai", "gpt-4o-mini", false, false);
    }

    @Test
    @DisplayName("compacts_window_and_stores_summary_when_called")
    void compacts_window_and_stores_summary_when_called() {
        InterviewRuntimeState state = freshState();
        CompactionSummaryResult summaryResult = new CompactionSummaryResult(
                List.of("GC"),
                List.of("STW 발생 시 처리량 손실"),
                List.of("기본 → 트레이드오프"),
                List.of("TRADEOFF"),
                List.of()
        );
        ChatResponse response = stubResponse("{\"covered_topics\":[\"GC\"]}");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(response);
        given(aiResponseParser.parseOrRetry(eq(response), eq(CompactionSummaryResult.class), eq(aiClient), any()))
                .willReturn(summaryResult);

        // @Async has no effect without Spring proxy — executes synchronously in test
        compactor.compactAsync(1L, 2, sampleExchanges(), state);

        assertThat(state.getCompactedSummary(2)).isPresent();
        assertThat(state.getCompactedSummary(2).get()).contains("GC");
    }

    @Test
    @DisplayName("is_idempotent_when_in_flight")
    void is_idempotent_when_in_flight() {
        InterviewRuntimeState state = freshState();
        state.markCompactionStarted(2); // mark already in-flight

        compactor.compactAsync(1L, 2, sampleExchanges(), state);

        verify(aiClient, never()).chat(any());
        assertThat(state.getCompactedSummary(2)).isEmpty();
    }

    @Test
    @DisplayName("does_not_throw_when_ai_call_fails")
    void does_not_throw_when_ai_call_fails() {
        InterviewRuntimeState state = freshState();
        given(aiClient.chat(any(ChatRequest.class))).willThrow(new RuntimeException("AI 장애"));

        assertThatCode(() -> compactor.compactAsync(1L, 2, sampleExchanges(), state))
                .doesNotThrowAnyException();

        assertThat(state.getCompactedSummary(2)).isEmpty();
    }

    @Test
    @DisplayName("marks_finished_after_completion")
    void marks_finished_after_completion() {
        InterviewRuntimeState state = freshState();
        CompactionSummaryResult summaryResult = new CompactionSummaryResult(
                List.of("topic"), List.of(), List.of(), List.of(), List.of()
        );
        ChatResponse response = stubResponse("{\"covered_topics\":[\"topic\"]}");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(response);
        given(aiResponseParser.parseOrRetry(eq(response), eq(CompactionSummaryResult.class), eq(aiClient), any()))
                .willReturn(summaryResult);

        compactor.compactAsync(1L, 2, sampleExchanges(), state);

        assertThat(state.hasCompactionInFlight(2)).isFalse();
    }

    @Test
    @DisplayName("marks_finished_even_when_ai_call_fails")
    void marks_finished_even_when_ai_call_fails() {
        InterviewRuntimeState state = freshState();
        given(aiClient.chat(any(ChatRequest.class))).willThrow(new RuntimeException("타임아웃"));

        compactor.compactAsync(1L, 2, sampleExchanges(), state);

        assertThat(state.hasCompactionInFlight(2)).isFalse();
    }
}
