package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiAnswerAnalyzerClient;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.AnswerAnalysisPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiAnswerAnalyzer — PromptBuilder 결과를 Client.call 인자로 전달하고 응답을 GeneratedAnswerAnalysis 로 매핑한다")
class OpenAiAnswerAnalyzerTest {

    private static final String VALID_JSON = """
            {
              "claims": [],
              "dimension_gaps": {"clarity": 2, "depth": 1},
              "weakest_dimension": "depth",
              "unstated_assumptions": [],
              "recommended_next_action": "DEEP_DIVE"
            }
            """;

    private OpenAiAnswerAnalyzerClient client;
    private AnswerAnalysisPromptBuilder promptBuilder;
    private OpenAiAnswerAnalyzer adapter;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiAnswerAnalyzerClient.class);
        promptBuilder = mock(AnswerAnalysisPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper());
        adapter = new OpenAiAnswerAnalyzer(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("PromptBuilder.build 결과 (system + user) 를 Client.call 에 그대로 전달한다")
    void analyze_passesPromptPairToClient() {
        when(promptBuilder.build(eq("주요 질문"), eq(ReferenceType.MODEL_ANSWER), eq("내 답변"), eq(QuestionCategory.CONCEPT)))
                .thenReturn(new AnswerAnalysisPromptBuilder.PromptPair("sys-p", "usr-p"));
        when(client.call(eq("sys-p"), eq("usr-p"))).thenReturn(VALID_JSON);

        adapter.analyze(1L, "주요 질문", ReferenceType.MODEL_ANSWER, "내 답변", QuestionCategory.CONCEPT);

        verify(client).call(eq("sys-p"), eq("usr-p"));
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedAnswerAnalysis 로 매핑한다")
    void analyze_mapsValidJson() {
        when(promptBuilder.build(any(), any(), any(), any(QuestionCategory.class)))
                .thenReturn(new AnswerAnalysisPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedAnswerAnalysis result = adapter.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", QuestionCategory.CONCEPT);

        assertThat(result.weakestDimension()).isEqualTo("depth");
        assertThat(result.dimensionGaps()).containsEntry("clarity", 2);
    }

    @Test
    @DisplayName("Client 응답이 깨진 JSON 이면 PARSE_FAILED 가 던져진다")
    void analyze_malformedJson_throwsParseFailed() {
        when(promptBuilder.build(any(), any(), any(), any(QuestionCategory.class)))
                .thenReturn(new AnswerAnalysisPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn("not a json");

        assertThatThrownBy(() -> adapter.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", QuestionCategory.CONCEPT))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);
    }
}
