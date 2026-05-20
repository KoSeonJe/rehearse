package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeFollowUpQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClaudeFollowUpQuestionGenerator — system prompt 에 JSON 강제 지시문을 prepend 한다")
class ClaudeFollowUpQuestionGeneratorTest {

    private static final String VALID_JSON = """
            {
              "skip": false,
              "question": "그 결정의 근거를 한 가지만 더 설명해 주시겠어요?",
              "tts_question": "그 결정의 근거를 한 가지만 더 설명해 주시겠어요?",
              "reason": "evidence 보완",
              "type": "DEEP_DIVE",
              "best_answer": null,
              "target_claim_idx": null
            }
            """;

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("evidence", 1), "evidence", List.of(), RecommendedNextAction.DEEP_DIVE);

    private ClaudeFollowUpQuestionGeneratorClient client;
    private FollowUpQuestionPromptBuilder promptBuilder;
    private ClaudeFollowUpQuestionGenerator adapter;

    @BeforeEach
    void setUp() {
        client = mock(ClaudeFollowUpQuestionGeneratorClient.class);
        promptBuilder = mock(FollowUpQuestionPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper(), null, null);
        adapter = new ClaudeFollowUpQuestionGenerator(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("system prompt 앞에 JSON 강제 지시문이 prepend 된다")
    void generate_prependsJsonInstructionToSystemPrompt() {
        when(promptBuilder.build(any(), any(), any(), any()))
                .thenReturn(new FollowUpQuestionPromptBuilder.PromptPair("base-sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        adapter.generate("Q", "A", SAMPLE_ANALYSIS, null);

        ArgumentCaptor<String> sysCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(sysCaptor.capture(), any());
        String captured = sysCaptor.getValue();
        assertThat(captured).contains("JSON");
        assertThat(captured).contains("base-sys");
        assertThat(captured.indexOf("JSON")).isLessThan(captured.indexOf("base-sys"));
    }

    @Test
    @DisplayName("user prompt 는 변경 없이 전달된다")
    void generate_userPromptPassedThrough() {
        when(promptBuilder.build(any(), any(), any(), any()))
                .thenReturn(new FollowUpQuestionPromptBuilder.PromptPair("sys", "user-content"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        adapter.generate("Q", "A", SAMPLE_ANALYSIS, null);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(any(), userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo("user-content");
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedFollowUp 으로 매핑하며 answerText 가 입력값으로 덮어쓰여진다")
    void generate_mapsValidJson() {
        when(promptBuilder.build(any(), any(), any(), any()))
                .thenReturn(new FollowUpQuestionPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedFollowUp result = adapter.generate("Q", "원문 답변", SAMPLE_ANALYSIS, null);

        assertThat(result.question()).contains("근거");
        assertThat(result.answerText()).isEqualTo("원문 답변");
    }
}
