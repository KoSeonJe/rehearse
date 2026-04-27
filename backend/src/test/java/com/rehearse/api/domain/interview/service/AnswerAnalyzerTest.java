package com.rehearse.api.domain.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerAnalyzer - 답변 분석 Step A")
class AnswerAnalyzerTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private InterviewContextBuilder contextBuilder;

    private AiResponseParser parser;
    private Cache<Long, InterviewRuntimeState> cache;
    private InterviewRuntimeStateStore runtimeStateStore;
    private AnswerAnalyzer analyzer;

    private static final BuiltContext STUB_CONTEXT = new BuiltContext(
            List.of(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, "system"),
                    ChatMessage.of(ChatMessage.Role.USER, "user")),
            100,
            Map.of("L1", 80, "L4", 20, "total", 100)
    );

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(new ObjectMapper());
        cache = Caffeine.newBuilder().<Long, InterviewRuntimeState>build();
        runtimeStateStore = new InterviewRuntimeStateStore(cache);
        analyzer = new AnswerAnalyzer(aiClient, parser, contextBuilder, runtimeStateStore);

        lenient().when(contextBuilder.build(any(ContextBuildRequest.class))).thenReturn(STUB_CONTEXT);
    }

    private InterviewRuntimeState seedState(Long interviewId) {
        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash-" + interviewId, ResumeSkeleton.CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);
        cache.put(interviewId, state);
        return state;
    }

    private static ChatResponse jsonResponse(String content) {
        return new ChatResponse(content, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }

    @Test
    @DisplayName("returns_parsed_record_and_caches_in_runtime_state_when_json_valid")
    void returns_parsed_record_and_caches_in_runtime_state_when_json_valid() {
        seedState(1L);
        String json = """
                {
                  "turn_id": 0,
                  "claims": [
                    {"text":"Young/Old GC 분리","depth_score":3,"evidence_strength":"WEAK","topic_tag":"gc"}
                  ],
                  "missing_perspectives": ["TRADEOFF","RELIABILITY"],
                  "unstated_assumptions": ["G1 가정"],
                  "answer_quality": 3,
                  "recommended_next_action": "DEEP_DIVE"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonResponse(json));

        AnswerAnalysis result = analyzer.analyze(1L, 100L, "GC 설명", ReferenceType.MODEL_ANSWER, "답변", List.of());

        assertThat(result.turnId()).isEqualTo(100L);
        assertThat(result.claims()).hasSize(1);
        assertThat(result.claims().get(0).depthScore()).isEqualTo(3);
        assertThat(result.claims().get(0).evidenceStrength()).isEqualTo(EvidenceStrength.WEAK);
        assertThat(result.missingPerspectives()).containsExactly(Perspective.TRADEOFF, Perspective.RELIABILITY);
        assertThat(result.answerQuality()).isEqualTo(3);
        assertThat(result.recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);

        Optional<AnswerAnalysis> cached = runtimeStateStore.get(1L).getAnswerAnalysis(100L);
        assertThat(cached).isPresent();
        assertThat(cached.get().turnId()).isEqualTo(100L);
        assertThat(cached.get().recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
    }

    @Test
    @DisplayName("forces_clarification_when_claims_empty_and_answer_quality_low")
    void forces_clarification_when_claims_empty_and_answer_quality_low() {
        seedState(2L);
        String json = """
                {
                  "turn_id": 0,
                  "claims": [],
                  "missing_perspectives": ["TRADEOFF"],
                  "unstated_assumptions": [],
                  "answer_quality": 1,
                  "recommended_next_action": "DEEP_DIVE"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonResponse(json));

        AnswerAnalysis result = analyzer.analyze(2L, 200L, "Q", ReferenceType.MODEL_ANSWER, "음...", List.of());

        assertThat(result.recommendedNextAction()).isEqualTo(RecommendedNextAction.CLARIFICATION);
        assertThat(runtimeStateStore.get(2L).getAnswerAnalysis(200L).get().recommendedNextAction())
                .isEqualTo(RecommendedNextAction.CLARIFICATION);
    }

    @Test
    @DisplayName("preserves_recommendation_when_claims_present_even_if_quality_low")
    void preserves_recommendation_when_claims_present_even_if_quality_low() {
        seedState(3L);
        String json = """
                {
                  "turn_id": 0,
                  "claims": [{"text":"부분답","depth_score":1,"evidence_strength":"ASSUMED","topic_tag":"x"}],
                  "missing_perspectives": [],
                  "unstated_assumptions": [],
                  "answer_quality": 1,
                  "recommended_next_action": "DEEP_DIVE"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonResponse(json));

        AnswerAnalysis result = analyzer.analyze(3L, 300L, "Q", ReferenceType.MODEL_ANSWER, "부분 답", List.of());

        assertThat(result.recommendedNextAction()).isEqualTo(RecommendedNextAction.DEEP_DIVE);
    }

    @Test
    @DisplayName("preserves_recommendation_when_quality_high_even_if_claims_empty")
    void preserves_recommendation_when_quality_high_even_if_claims_empty() {
        seedState(4L);
        String json = """
                {
                  "turn_id": 0,
                  "claims": [],
                  "missing_perspectives": [],
                  "unstated_assumptions": [],
                  "answer_quality": 4,
                  "recommended_next_action": "SKIP"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonResponse(json));

        AnswerAnalysis result = analyzer.analyze(4L, 400L, "Q", ReferenceType.MODEL_ANSWER, "답변", List.of());

        assertThat(result.recommendedNextAction()).isEqualTo(RecommendedNextAction.SKIP);
    }

    @Test
    @DisplayName("overrides_turn_id_from_prompt_default_zero")
    void overrides_turn_id_from_prompt_default_zero() {
        seedState(5L);
        String json = """
                {
                  "turn_id": 0,
                  "claims": [{"text":"a","depth_score":2,"evidence_strength":"WEAK","topic_tag":"t"}],
                  "missing_perspectives": [],
                  "unstated_assumptions": [],
                  "answer_quality": 3,
                  "recommended_next_action": "DEEP_DIVE"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonResponse(json));

        AnswerAnalysis result = analyzer.analyze(5L, 999L, "Q", ReferenceType.MODEL_ANSWER, "답변", List.of());

        assertThat(result.turnId()).isEqualTo(999L);
    }

    @Test
    @DisplayName("retries_on_first_parse_failure_via_parseOrRetry")
    void retries_on_first_parse_failure_via_parseOrRetry() {
        seedState(6L);
        String invalidJson = "이건 JSON 이 아닙니다";
        String validJson = """
                {
                  "turn_id": 0,
                  "claims": [{"text":"재시도 성공","depth_score":3,"evidence_strength":"STRONG","topic_tag":"retry"}],
                  "missing_perspectives": [],
                  "unstated_assumptions": [],
                  "answer_quality": 3,
                  "recommended_next_action": "DEEP_DIVE"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class)))
                .willReturn(jsonResponse(invalidJson))
                .willReturn(jsonResponse(validJson));

        AnswerAnalysis result = analyzer.analyze(6L, 600L, "Q", ReferenceType.MODEL_ANSWER, "답변", List.of());

        assertThat(result.claims()).hasSize(1);
        assertThat(result.claims().get(0).topicTag()).isEqualTo("retry");
    }

    @Test
    @DisplayName("throws_when_interview_id_or_turn_id_is_null")
    void throws_when_interview_id_or_turn_id_is_null() {
        assertThatThrownBy(() -> analyzer.analyze(null, 1L, "Q", ReferenceType.MODEL_ANSWER, "A", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> analyzer.analyze(1L, null, "Q", ReferenceType.MODEL_ANSWER, "A", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("contextBuilder_invoked_with_answer_analyzer_callType_and_correct_focusHints")
    void contextBuilder_invoked_with_answer_analyzer_callType_and_correct_focusHints() {
        seedState(7L);
        String json = """
                {
                  "turn_id": 0,
                  "claims": [{"text":"a","depth_score":2,"evidence_strength":"WEAK","topic_tag":"t"}],
                  "missing_perspectives": [],
                  "unstated_assumptions": [],
                  "answer_quality": 3,
                  "recommended_next_action": "DEEP_DIVE"
                }
                """;
        given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonResponse(json));

        ArgumentCaptor<ContextBuildRequest> captor = ArgumentCaptor.forClass(ContextBuildRequest.class);
        given(contextBuilder.build(captor.capture())).willReturn(STUB_CONTEXT);

        analyzer.analyze(7L, 700L, "GC 설명", ReferenceType.MODEL_ANSWER, "답변", List.of());

        ContextBuildRequest captured = captor.getValue();
        assertThat(captured.callType()).isEqualTo("answer_analyzer");
        assertThat(captured.focusHints()).containsKey("mainQuestion");
        assertThat(captured.focusHints()).containsKey("userAnswer");
        assertThat(captured.focusHints()).containsKey("personaDepthHint");
    }
}
