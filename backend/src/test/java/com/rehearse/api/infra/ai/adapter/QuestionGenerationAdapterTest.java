package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import com.rehearse.api.infra.ai.schema.GeneratedQuestionsWrapperSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * QuestionGenerationAdapter — generateQuestions() 가 AiClient.chat() 경유로 동작하는지 검증.
 *
 * <p>plan 검증 #3/#6: legacy generateQuestions() 호출 시 실제로 chat() 경유로 변환됨을 증명.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionGenerationAdapter — chat() 경유 질문 생성 검증")
class QuestionGenerationAdapterTest {

    @Mock private QuestionGenerationPromptBuilder promptBuilder;
    @Mock private AiResponseParser responseParser;
    @Mock private AiClient aiClient;

    private QuestionGenerationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QuestionGenerationAdapter(promptBuilder, responseParser);
    }

    private QuestionGenerationRequest questionRequest() {
        return new QuestionGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL), null, null, 30, null);
    }

    private GeneratedQuestion sampleQuestion() {
        return new GeneratedQuestion(
                "질문", "tts", "category", 1, "criteria", "CS_FUNDAMENTAL", "model", "strategy");
    }

    @Test
    @DisplayName("adapt() 호출 시 AiClient.chat() 이 한 번 호출된다")
    void adapt_callsAiClientChat() {
        // given
        QuestionGenerationRequest req = questionRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("system prompt");
        given(promptBuilder.buildUserPrompt(req)).willReturn("user prompt");

        ChatResponse mockResponse = new ChatResponse(
                "{\"questions\":[{\"content\":\"q1\",\"order\":1}]}", ChatResponse.Usage.empty(),
                "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockResponse);

        GeneratedQuestionsWrapper realWrapper = new GeneratedQuestionsWrapper(List.of(sampleQuestion()));
        given(responseParser.parseOrRetry(any(), eq(GeneratedQuestionsWrapper.class), eq(aiClient), any()))
                .willReturn(realWrapper);

        // when
        List<GeneratedQuestion> result = adapter.adapt(aiClient, req);

        // then — chat() 가 호출됐는지 검증
        verify(aiClient).chat(any(ChatRequest.class));
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("adapt() 이 빌드하는 ChatRequest 는 callType=generate_questions 이고 strict JSON_SCHEMA 포맷이다")
    void adapt_chatRequestHasCorrectCallTypeAndFormat() {
        // given
        QuestionGenerationRequest req = questionRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        ChatResponse mockResponse = new ChatResponse("{}",
                ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(captor.capture())).willReturn(mockResponse);

        GeneratedQuestionsWrapper wrapper = new GeneratedQuestionsWrapper(List.of(sampleQuestion()));
        given(responseParser.parseOrRetry(any(), eq(GeneratedQuestionsWrapper.class), eq(aiClient), any()))
                .willReturn(wrapper);

        // when
        adapter.adapt(aiClient, req);

        // then
        ChatRequest captured = captor.getValue();
        assertThat(captured.callType()).isEqualTo("generate_questions");
        assertThat(captured.responseFormat()).isEqualTo(ResponseFormat.JSON_SCHEMA);
        assertThat(captured.jsonSchema()).isNotNull();
        assertThat(captured.jsonSchema().name()).isEqualTo(GeneratedQuestionsWrapperSchema.SCHEMA_NAME);
        assertThat(captured.jsonSchema().schema()).containsEntry("type", "object");
        assertThat(captured.jsonSchema().schema()).containsEntry("additionalProperties", false);
        assertThat(captured.jsonSchema().schema().get("required")).isEqualTo(List.of("questions"));
    }

    @Test
    @DisplayName("buildJsonSchema() 는 question 객체 항목별 7개 필드를 required 로 포함한다")
    void buildJsonSchema_requiresAllQuestionFields() {
        // when
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> schema =
                (java.util.Map<String, Object>) GeneratedQuestionsWrapperSchema.build();

        // then
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties = (java.util.Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> questionsArray = (java.util.Map<String, Object>) properties.get("questions");
        assertThat(questionsArray).containsEntry("type", "array");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> itemSchema = (java.util.Map<String, Object>) questionsArray.get("items");
        assertThat(itemSchema).containsEntry("type", "object");
        assertThat(itemSchema).containsEntry("additionalProperties", false);
        assertThat(itemSchema.get("required")).isEqualTo(List.of(
                "content", "tts_content", "category", "order",
                "evaluation_criteria", "question_category", "best_answer"));
    }

    @Test
    @DisplayName("adapt() 이 빌드하는 ChatRequest 는 SYSTEM + USER 두 개 메시지를 포함한다")
    void adapt_chatRequestHasSystemAndUserMessages() {
        // given
        QuestionGenerationRequest req = questionRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys-content");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr-content");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        ChatResponse mockResponse = new ChatResponse("{}",
                ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(captor.capture())).willReturn(mockResponse);

        GeneratedQuestionsWrapper wrapper = new GeneratedQuestionsWrapper(List.of(sampleQuestion()));
        given(responseParser.parseOrRetry(any(), eq(GeneratedQuestionsWrapper.class), eq(aiClient), any()))
                .willReturn(wrapper);

        // when
        adapter.adapt(aiClient, req);

        // then
        ChatRequest captured = captor.getValue();
        assertThat(captured.messages()).hasSize(2);
        assertThat(captured.messages().get(0).role()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(captured.messages().get(1).role()).isEqualTo(ChatMessage.Role.USER);
        assertThat(captured.messages().get(0).content()).isEqualTo("sys-content");
        assertThat(captured.messages().get(1).content()).isEqualTo("usr-content");
    }
}
