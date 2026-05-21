package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiFollowUpQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiFollowUpQuestionGenerator — PromptBuilder 결과를 Client.call 에 전달하고 응답을 GeneratedFollowUp 으로 매핑한다")
class OpenAiFollowUpQuestionGeneratorTest {

    private static final String VALID_JSON = """
            {
              "skip": false,
              "question": "구체적인 예시를 한 가지만 들어 설명해 주시겠어요?",
              "tts_question": "구체적인 예시를 한 가지만 들어 설명해 주시겠어요?",
              "reason": "depth 보완",
              "type": "DEEP_DIVE",
              "best_answer": null,
              "target_claim_idx": 0
            }
            """;

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);

    private OpenAiFollowUpQuestionGeneratorClient client;
    private FollowUpQuestionPromptBuilder promptBuilder;
    private OpenAiFollowUpQuestionGenerator adapter;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiFollowUpQuestionGeneratorClient.class);
        promptBuilder = mock(FollowUpQuestionPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper());
        adapter = new OpenAiFollowUpQuestionGenerator(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("PromptBuilder.build 결과 (system + user) 를 Client.call 에 그대로 전달한다")
    void generate_passesPromptPairToClient() {
        when(promptBuilder.build(eq("주요 질문"), eq("내 답변"), eq(SAMPLE_ANALYSIS)))
                .thenReturn(new FollowUpQuestionPromptBuilder.PromptPair("sys-p", "usr-p"));
        when(client.call(eq("sys-p"), eq("usr-p"))).thenReturn(VALID_JSON);

        adapter.generate("주요 질문", "내 답변", SAMPLE_ANALYSIS);

        verify(client).call(eq("sys-p"), eq("usr-p"));
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedFollowUp 으로 매핑하며 answerText 가 입력값으로 덮어쓰여진다")
    void generate_mapsValidJsonAndOverridesAnswerText() {
        when(promptBuilder.build(any(), any(), any()))
                .thenReturn(new FollowUpQuestionPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedFollowUp result = adapter.generate("Q", "원문 답변", SAMPLE_ANALYSIS);

        assertThat(result.question()).contains("구체적인 예시");
        assertThat(result.answerText()).isEqualTo("원문 답변");
        assertThat(result.targetClaimIdx()).isZero();
    }

    @Test
    @DisplayName("Client 응답이 깨진 JSON 이면 PARSE_FAILED 가 던져진다")
    void generate_malformedJson_throwsParseFailed() {
        when(promptBuilder.build(any(), any(), any()))
                .thenReturn(new FollowUpQuestionPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn("not a json");

        assertThatThrownBy(() -> adapter.generate("Q", "A", SAMPLE_ANALYSIS))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);
    }
}
