package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeAnswerAnalyzerClient;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.prompt.AnswerAnalysisPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClaudeAnswerAnalyzer — system prompt 에 JSON 강제 지시문을 prepend 한다")
class ClaudeAnswerAnalyzerTest {

    private static final String VALID_JSON = """
            {
              "claims": [],
              "dimension_gaps": {"clarity": 1},
              "weakest_dimension": "clarity",
              "unstated_assumptions": [],
              "recommended_next_action": "CLARIFICATION"
            }
            """;

    private ClaudeAnswerAnalyzerClient client;
    private AnswerAnalysisPromptBuilder promptBuilder;
    private ClaudeAnswerAnalyzer adapter;

    @BeforeEach
    void setUp() {
        client = mock(ClaudeAnswerAnalyzerClient.class);
        promptBuilder = mock(AnswerAnalysisPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper());
        adapter = new ClaudeAnswerAnalyzer(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("system prompt 앞에 JSON 강제 지시문이 prepend 된다")
    void analyze_prependsJsonInstructionToSystemPrompt() {
        when(promptBuilder.build(any(), any(), any()))
                .thenReturn(new AnswerAnalysisPromptBuilder.PromptPair("base-sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        adapter.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A");

        ArgumentCaptor<String> sysCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(sysCaptor.capture(), any());
        String captured = sysCaptor.getValue();
        assertThat(captured).contains("JSON");
        assertThat(captured).contains("base-sys");
        assertThat(captured.indexOf("JSON")).isLessThan(captured.indexOf("base-sys"));
    }

    @Test
    @DisplayName("user prompt 는 변경 없이 전달된다")
    void analyze_userPromptPassedThrough() {
        when(promptBuilder.build(any(), any(), any()))
                .thenReturn(new AnswerAnalysisPromptBuilder.PromptPair("sys", "user-content"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        adapter.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A");

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(any(), userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo("user-content");
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedAnswerAnalysis 로 매핑한다")
    void analyze_mapsValidJson() {
        when(promptBuilder.build(any(), any(), any()))
                .thenReturn(new AnswerAnalysisPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedAnswerAnalysis result = adapter.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A");

        assertThat(result.weakestDimension()).isEqualTo("clarity");
    }
}
