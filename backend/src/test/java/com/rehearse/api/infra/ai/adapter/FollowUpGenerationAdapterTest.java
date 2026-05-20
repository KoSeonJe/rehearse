package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import com.rehearse.api.infra.ai.schema.GeneratedFollowUpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpGenerationAdapter — strict JSON Schema 적용 검증")
class FollowUpGenerationAdapterTest {

    @Mock private FollowUpPromptBuilder promptBuilder;
    @Mock private AiResponseParser responseParser;
    @Mock private AiClient aiClient;

    private FollowUpGenerationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FollowUpGenerationAdapter(promptBuilder, responseParser);
    }

    private FollowUpGenerationRequest followUpRequest() {
        return new FollowUpGenerationRequest(
                Position.BACKEND, TechStack.JAVA_SPRING, InterviewLevel.MID,
                "메인 질문", "답변", null, List.of(), null);
    }

    private GeneratedFollowUp sampleFollowUp() {
        return new GeneratedFollowUp(false, null, "후속 질문", "TTS", "이유", "DEEP_DIVE",
                "best", "", 0);
    }

    @Test
    @DisplayName("adapt() ChatRequest 는 callType=generate_followup + strict JSON_SCHEMA 포맷이다")
    void adapt_chatRequest_uses_strict_json_schema() {
        FollowUpGenerationRequest req = followUpRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        ChatResponse mockResponse = new ChatResponse("{}",
                ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(captor.capture())).willReturn(mockResponse);
        given(responseParser.parseOrRetry(any(), eq(GeneratedFollowUp.class), eq(aiClient), any()))
                .willReturn(sampleFollowUp());

        adapter.adapt(aiClient, req);

        ChatRequest captured = captor.getValue();
        assertThat(captured.callType()).isEqualTo("generate_followup");
        assertThat(captured.responseFormat()).isEqualTo(ResponseFormat.JSON_SCHEMA);
        assertThat(captured.jsonSchema()).isNotNull();
        assertThat(captured.jsonSchema().name()).isEqualTo(GeneratedFollowUpSchema.NAME);
        assertThat(captured.jsonSchema().schema()).containsEntry("type", "object");
        assertThat(captured.jsonSchema().schema()).containsEntry("additionalProperties", false);
    }

    @Test
    @DisplayName("buildJsonSchema() 는 9개 필드를 모두 required 로 포함하고 nullable 필드를 type 배열로 표현한다")
    void buildJsonSchema_includesAllFields_withNullableTypes() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) GeneratedFollowUpSchema.build();

        assertThat(schema.get("required")).isEqualTo(List.of(
                "skip", "skip_reason", "question", "tts_question",
                "reason", "type", "best_answer", "answer_text", "target_claim_idx"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> skip = (Map<String, Object>) properties.get("skip");
        assertThat(skip.get("type")).isEqualTo("boolean");

        @SuppressWarnings("unchecked")
        Map<String, Object> question = (Map<String, Object>) properties.get("question");
        assertThat(question.get("type")).isEqualTo(List.of("string", "null"));

        @SuppressWarnings("unchecked")
        Map<String, Object> targetClaimIdx = (Map<String, Object>) properties.get("target_claim_idx");
        assertThat(targetClaimIdx.get("type")).isEqualTo(List.of("integer", "null"));

        @SuppressWarnings("unchecked")
        Map<String, Object> answerText = (Map<String, Object>) properties.get("answer_text");
        assertThat(answerText.get("type")).isEqualTo("string");
    }
}
