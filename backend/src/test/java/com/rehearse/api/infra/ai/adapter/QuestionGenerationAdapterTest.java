package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
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

        // Use a real wrapper with questions to pass the null check
        GeneratedQuestion question = new GeneratedQuestion();
        GeneratedQuestionsWrapper realWrapper = new GeneratedQuestionsWrapper();
        java.lang.reflect.Field f;
        try {
            f = GeneratedQuestionsWrapper.class.getDeclaredField("questions");
            f.setAccessible(true);
            f.set(realWrapper, List.of(question));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        given(responseParser.parseOrRetry(any(), eq(GeneratedQuestionsWrapper.class), eq(aiClient), any()))
                .willReturn(realWrapper);

        // when
        List<GeneratedQuestion> result = adapter.adapt(aiClient, req);

        // then — chat() 가 호출됐는지 검증
        verify(aiClient).chat(any(ChatRequest.class));
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("adapt() 이 빌드하는 ChatRequest 는 callType=generate_questions 이고 JSON_OBJECT 포맷이다")
    void adapt_chatRequestHasCorrectCallTypeAndFormat() {
        // given
        QuestionGenerationRequest req = questionRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        ChatResponse mockResponse = new ChatResponse("{}",
                ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(captor.capture())).willReturn(mockResponse);

        GeneratedQuestionsWrapper wrapper = new GeneratedQuestionsWrapper();
        try {
            var f = GeneratedQuestionsWrapper.class.getDeclaredField("questions");
            f.setAccessible(true);
            f.set(wrapper, List.of(new GeneratedQuestion()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        given(responseParser.parseOrRetry(any(), eq(GeneratedQuestionsWrapper.class), eq(aiClient), any()))
                .willReturn(wrapper);

        // when
        adapter.adapt(aiClient, req);

        // then
        ChatRequest captured = captor.getValue();
        assertThat(captured.callType()).isEqualTo("generate_questions");
        assertThat(captured.responseFormat()).isEqualTo(ResponseFormat.JSON_OBJECT);
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

        GeneratedQuestionsWrapper wrapper = new GeneratedQuestionsWrapper();
        try {
            var f = GeneratedQuestionsWrapper.class.getDeclaredField("questions");
            f.setAccessible(true);
            f.set(wrapper, List.of(new GeneratedQuestion()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
