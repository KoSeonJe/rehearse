package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.SttService;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FollowUpGenerationAdapter — generateFollowUpQuestion() / generateFollowUpWithAudio() 가
 * AiClient.chat() 경유로 동작하는지 검증.
 *
 * <p>plan 검증 #3: legacy 3개 메서드가 chat() 경유로 변환됨을 증명.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpGenerationAdapter — chat() 경유 후속 질문 생성 검증")
class FollowUpGenerationAdapterTest {

    @Mock private FollowUpPromptBuilder promptBuilder;
    @Mock private AiResponseParser responseParser;
    @Mock private AiClient aiClient;
    @Mock private SttService sttService;

    private FollowUpGenerationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FollowUpGenerationAdapter(promptBuilder, responseParser);
    }

    private FollowUpGenerationRequest followUpRequest() {
        return new FollowUpGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                "질문 내용", "답변 내용", null, null, null);
    }

    private GeneratedFollowUp stubFollowUp() {
        GeneratedFollowUp f = new GeneratedFollowUp();
        return f.withAnswerText("답변");
    }

    @Test
    @DisplayName("adapt() 호출 시 AiClient.chat() 이 한 번 호출된다")
    void adapt_callsAiClientChat() {
        // given
        FollowUpGenerationRequest req = followUpRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr");

        ChatResponse mockResponse = new ChatResponse("{}", ChatResponse.Usage.empty(),
                "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockResponse);
        given(responseParser.parseOrRetry(any(), eq(GeneratedFollowUp.class), eq(aiClient), any()))
                .willReturn(stubFollowUp());

        // when
        adapter.adapt(aiClient, req);

        // then
        verify(aiClient).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("adapt() 이 빌드하는 ChatRequest 는 callType=generate_followup 이다")
    void adapt_chatRequestCallType() {
        // given
        FollowUpGenerationRequest req = followUpRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        ChatResponse mockResponse = new ChatResponse("{}", ChatResponse.Usage.empty(),
                "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(captor.capture())).willReturn(mockResponse);
        given(responseParser.parseOrRetry(any(), eq(GeneratedFollowUp.class), eq(aiClient), any()))
                .willReturn(stubFollowUp());

        // when
        adapter.adapt(aiClient, req);

        // then
        assertThat(captor.getValue().callType()).isEqualTo("generate_followup");
    }

    @Test
    @DisplayName("adaptWithAudio() — STT 결과가 answerText 로 설정되고 chat() 이 호출된다")
    void adaptWithAudio_sttResultUsedAndChatCalled() {
        // given
        FollowUpGenerationRequest req = followUpRequest();
        MockMultipartFile audioFile = new MockMultipartFile("audio", "test.webm",
                "audio/webm", new byte[]{1, 2, 3});

        String transcribed = "STT 변환 텍스트";
        given(sttService.transcribe(audioFile)).willReturn(transcribed);

        given(promptBuilder.buildSystemPrompt(any())).willReturn("sys");
        given(promptBuilder.buildUserPrompt(any())).willReturn("usr");

        ChatResponse mockResponse = new ChatResponse("{}", ChatResponse.Usage.empty(),
                "claude", "claude-haiku-4-5-20251001", false, false);
        given(aiClient.chat(any(ChatRequest.class))).willReturn(mockResponse);

        GeneratedFollowUp base = stubFollowUp();
        given(responseParser.parseOrRetry(any(), eq(GeneratedFollowUp.class), eq(aiClient), any()))
                .willReturn(base);

        // when
        GeneratedFollowUp result = adapter.adaptWithAudio(aiClient, audioFile, req, sttService);

        // then — STT 경유 후 chat() 가 호출됐음을 검증
        verify(sttService).transcribe(audioFile);
        verify(aiClient).chat(any(ChatRequest.class));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("adapt() 이 빌드하는 ChatRequest 는 JSON_OBJECT 포맷이다")
    void adapt_chatRequestJsonObjectFormat() {
        // given
        FollowUpGenerationRequest req = followUpRequest();
        given(promptBuilder.buildSystemPrompt(req)).willReturn("sys");
        given(promptBuilder.buildUserPrompt(req)).willReturn("usr");

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        ChatResponse mockResponse = new ChatResponse("{}", ChatResponse.Usage.empty(),
                "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(captor.capture())).willReturn(mockResponse);
        given(responseParser.parseOrRetry(any(), eq(GeneratedFollowUp.class), eq(aiClient), any()))
                .willReturn(stubFollowUp());

        // when
        adapter.adapt(aiClient, req);

        // then
        assertThat(captor.getValue().responseFormat()).isEqualTo(ResponseFormat.JSON_OBJECT);
    }
}
