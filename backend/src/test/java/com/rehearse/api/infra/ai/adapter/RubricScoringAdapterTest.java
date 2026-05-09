package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RubricScoringAdapter")
class RubricScoringAdapterTest {

    @InjectMocks
    private RubricScoringAdapter adapter;

    @Mock
    private AiResponseParser responseParser;

    @Mock
    private AiClient aiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void injectObjectMapper() throws Exception {
        var field = RubricScoringAdapter.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(adapter, objectMapper);
    }

    @Nested
    @DisplayName("정상 파싱")
    class NormalParsing {

        @Test
        @DisplayName("score 1~3 범위, evidence_quote 존재 → 정상 RubricScore 반환")
        void adapt_validResponse_returnsCorrectScore() {
            String json = "{\"technical_depth\":{\"score\":2,\"observation\":\"설명\",\"evidence_quote\":\"증거텍스트\"}}";
            ChatResponse response = mockChatResponse("content");
            given(aiClient.chat(any())).willReturn(response);
            given(responseParser.extractJson("content")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("technical_depth"));

            assertThat(result.scoredDimensions()).containsExactly("technical_depth");
            assertThat(result.dimensionScores().get("technical_depth").score()).isEqualTo(2);
            assertThat(result.dimensionScores().get("technical_depth").evidenceQuote()).isEqualTo("증거텍스트");
        }
    }

    @Nested
    @DisplayName("score 범위 검증 — 매핑 거절 → schema retry")
    class ScoreRangeValidation {

        @Test
        @DisplayName("score=0 → record 거절 → schema retry → 정상 score 수신")
        void adapt_scoreBelowMin_triggersSchemaRetry() {
            String firstJson = "{\"technical_depth\":{\"score\":0,\"observation\":\"범위 밖\",\"evidence_quote\":\"ev\"}}";
            String retryJson = "{\"technical_depth\":{\"score\":2,\"observation\":\"수정\",\"evidence_quote\":\"ev2\"}}";
            ChatResponse firstResponse = mockChatResponse("first");
            ChatResponse retryResponse = mockChatResponse("retry");
            given(aiClient.chat(any())).willReturn(firstResponse).willReturn(retryResponse);
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("technical_depth"));

            verify(aiClient, times(2)).chat(any());
            assertThat(result.dimensionScores().get("technical_depth").score()).isEqualTo(2);
        }

        @Test
        @DisplayName("score=4 → record 거절 → schema retry 도 실패 → fallback (notApplicable)")
        void adapt_scoreAboveMax_retryFails_fallback() {
            String firstJson = "{\"reasoning_communication\":{\"score\":4,\"observation\":\"범위 초과\",\"evidence_quote\":\"ev\"}}";
            String retryJson = "{\"reasoning_communication\":{\"score\":5,\"observation\":\"여전히 초과\",\"evidence_quote\":\"ev\"}}";
            ChatResponse firstResponse = mockChatResponse("first");
            ChatResponse retryResponse = mockChatResponse("retry");
            given(aiClient.chat(any())).willReturn(firstResponse).willReturn(retryResponse);
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("reasoning_communication"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("reasoning_communication"));

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("reasoning_communication").score()).isNull();
            assertThat(result.dimensionScores().get("reasoning_communication").observation()).contains("파싱 실패");
        }
    }

    @Nested
    @DisplayName("evidence_quote null retry (B10)")
    class EvidenceQuoteRetry {

        @Test
        @DisplayName("evidence_quote null이면 retry 후 그래도 null이면 score null 처리")
        void adapt_missingEvidenceQuote_retriesAndNullifiesScore() {
            String firstJson = "{\"technical_depth\":{\"score\":2,\"observation\":\"설명\",\"evidence_quote\":null}}";
            String retryJson = "{\"technical_depth\":{\"score\":2,\"observation\":\"설명\",\"evidence_quote\":null}}";

            ChatResponse firstResponse = mockChatResponse("first");
            ChatResponse retryResponse = mockChatResponse("retry");

            given(aiClient.chat(any()))
                    .willReturn(firstResponse)
                    .willReturn(retryResponse);
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("technical_depth"));

            assertThat(result.scoredDimensions()).doesNotContain("technical_depth");
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(result.dimensionScores().get("technical_depth").observation()).contains("evidence_quote 누락으로 score 무효화");
        }
    }

    @Nested
    @DisplayName("fallback 정책 보존")
    class FallbackPolicy {

        @Test
        @DisplayName("LLM 응답에 차원 누락 → notApplicable 로 채움 (record 거절 X — score=null 허용)")
        void adapt_missingDimension_fillsAsNotApplicable() {
            String json = "{}";
            ChatResponse response = mockChatResponse("content");
            given(aiClient.chat(any())).willReturn(response);
            given(responseParser.extractJson("content")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("technical_depth"));

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(result.dimensionScores().get("technical_depth").observation()).contains("LLM 응답에 차원 없음");
        }
    }

    private ChatResponse mockChatResponse(String content) {
        return new ChatResponse(content, null, "openai", "gpt-4o-mini", false, false);
    }

    private ChatRequest mockRequest() {
        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "test")))
                .callType("rubric_scorer")
                .build();
    }

    private Rubric createRubric(String rubricId, List<String> dimRefs) {
        List<DimensionRef> dims = dimRefs.stream()
                .map(ref -> new DimensionRef(ref, 1.0))
                .toList();
        return new Rubric(rubricId, "테스트", dims, Map.of(), Map.of());
    }
}
