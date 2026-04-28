package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
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
            String json = "{\"D2\":{\"score\":2,\"observation\":\"설명\",\"evidence_quote\":\"증거텍스트\"}}";
            ChatResponse response = mockChatResponse("content");
            given(aiClient.chat(any())).willReturn(response);
            given(responseParser.extractJson("content")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("D2"));
            RubricScore result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("D2"));

            assertThat(result.scoredDimensions()).containsExactly("D2");
            assertThat(result.dimensionScores().get("D2").score()).isEqualTo(2);
            assertThat(result.dimensionScores().get("D2").evidenceQuote()).isEqualTo("증거텍스트");
        }
    }

    @Nested
    @DisplayName("score 범위 검증 (B9)")
    class ScoreRangeValidation {

        @Test
        @DisplayName("score=0 (범위 밖) → null로 처리되어 scoredDimensions에 포함 안 됨")
        void adapt_scoreBelowMin_nullifies() {
            String json = "{\"D2\":{\"score\":0,\"observation\":\"범위 밖\",\"evidence_quote\":\"ev\"}}";
            ChatResponse response = mockChatResponse("content");
            given(aiClient.chat(any())).willReturn(response);
            given(responseParser.extractJson("content")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("D2"));
            RubricScore result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("D2"));

            assertThat(result.scoredDimensions()).doesNotContain("D2");
            assertThat(result.dimensionScores().get("D2").score()).isNull();
        }

        @Test
        @DisplayName("score=4 (범위 밖) → null로 처리되어 scoredDimensions에 포함 안 됨")
        void adapt_scoreAboveMax_nullifies() {
            String json = "{\"D3\":{\"score\":4,\"observation\":\"범위 초과\",\"evidence_quote\":\"ev\"}}";
            ChatResponse response = mockChatResponse("content");
            given(aiClient.chat(any())).willReturn(response);
            given(responseParser.extractJson("content")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("D3"));
            RubricScore result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("D3"));

            assertThat(result.scoredDimensions()).doesNotContain("D3");
            assertThat(result.dimensionScores().get("D3").score()).isNull();
        }
    }

    @Nested
    @DisplayName("evidence_quote null retry (B10)")
    class EvidenceQuoteRetry {

        @Test
        @DisplayName("evidence_quote null이면 retry 후 그래도 null이면 score null 처리")
        void adapt_missingEvidenceQuote_retriesAndNullifiesScore() {
            String firstJson = "{\"D2\":{\"score\":2,\"observation\":\"설명\",\"evidence_quote\":null}}";
            String retryJson = "{\"D2\":{\"score\":2,\"observation\":\"설명\",\"evidence_quote\":null}}";

            ChatResponse firstResponse = mockChatResponse("first");
            ChatResponse retryResponse = mockChatResponse("retry");

            given(aiClient.chat(any()))
                    .willReturn(firstResponse)
                    .willReturn(retryResponse);
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("D2"));
            RubricScore result = adapter.adapt(aiClient, mockRequest(), rubric, List.of("D2"));

            assertThat(result.scoredDimensions()).doesNotContain("D2");
            assertThat(result.dimensionScores().get("D2").score()).isNull();
            assertThat(result.dimensionScores().get("D2").observation()).contains("evidence_quote 누락으로 score 무효화");
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
