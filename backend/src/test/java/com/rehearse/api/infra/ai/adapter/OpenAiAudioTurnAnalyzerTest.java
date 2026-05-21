package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiAudioTurnAnalyzerClient;
import com.rehearse.api.infra.ai.dto.GeneratedTurnAnalysis;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import com.rehearse.api.infra.ai.prompt.AudioTurnAnalyzerPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiAudioTurnAnalyzer — PromptBuilder + Client 위임 + 인프라/네트워크 오류를 AudioChatFallbackRequiredException 으로 변환")
class OpenAiAudioTurnAnalyzerTest {

    private static final String VALID_JSON = """
            {
              "answer_analysis": {
                "claims": [],
                "dimension_gaps": {"depth": 1},
                "weakest_dimension": "depth",
                "unstated_assumptions": [],
                "recommended_next_action": "DEEP_DIVE"
              }
            }
            """;

    private OpenAiAudioTurnAnalyzerClient client;
    private AudioTurnAnalyzerPromptBuilder promptBuilder;
    private OpenAiAudioTurnAnalyzer adapter;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiAudioTurnAnalyzerClient.class);
        promptBuilder = mock(AudioTurnAnalyzerPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper());
        adapter = new OpenAiAudioTurnAnalyzer(client, promptBuilder, parser);
    }

    @Test
    @DisplayName("PromptBuilder system + user 결과를 Client.call 에 전달하고 정상 JSON 을 GeneratedTurnAnalysis 로 매핑한다")
    void analyze_passesPromptsAndMapsJson() {
        when(promptBuilder.buildSystemPrompt()).thenReturn("sys-prompt");
        when(promptBuilder.buildUserPromptText(eq("주요 질문"), eq(ReferenceType.MODEL_ANSWER)))
                .thenReturn("user-prompt");
        MultipartFile audio = audioFile();
        when(client.call(eq("sys-prompt"), eq("user-prompt"), eq(audio))).thenReturn(VALID_JSON);

        GeneratedTurnAnalysis result = adapter.analyze(audio, "주요 질문", ReferenceType.MODEL_ANSWER, false);

        verify(client).call(eq("sys-prompt"), eq("user-prompt"), eq(audio));
        assertThat(result.answerAnalysis()).isNotNull();
        assertThat(result.toDomain().weakestDimension()).isEqualTo("depth");
    }

    @Test
    @DisplayName("Client 가 PARSE_FAILED BusinessException 던지면 rethrow — 응답 구조 결함은 text fallback 도 위험")
    void analyze_parseFailed_rethrows() {
        when(promptBuilder.buildSystemPrompt()).thenReturn("sys");
        when(promptBuilder.buildUserPromptText(any(), any())).thenReturn("usr");
        when(client.call(any(), any(), any())).thenThrow(new BusinessException(AiErrorCode.PARSE_FAILED));

        assertThatThrownBy(() -> adapter.analyze(audioFile(), "Q", ReferenceType.GUIDE, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);
    }

    @Test
    @DisplayName("Client 가 CLIENT_ERROR BusinessException 던지면 AudioChatFallbackRequiredException 으로 변환")
    void analyze_clientError_throwsFallbackSignal() {
        when(promptBuilder.buildSystemPrompt()).thenReturn("sys");
        when(promptBuilder.buildUserPromptText(any(), any())).thenReturn("usr");
        when(client.call(any(), any(), any())).thenThrow(new BusinessException(AiErrorCode.CLIENT_ERROR));

        assertThatThrownBy(() -> adapter.analyze(audioFile(), "Q", ReferenceType.GUIDE, false))
                .isInstanceOf(AudioChatFallbackRequiredException.class);
    }

    @Test
    @DisplayName("Client 가 RetryableApiException (5xx 누적) 던지면 AudioChatFallbackRequiredException 으로 변환")
    void analyze_retryableApiException_throwsFallbackSignal() {
        when(promptBuilder.buildSystemPrompt()).thenReturn("sys");
        when(promptBuilder.buildUserPromptText(any(), any())).thenReturn("usr");
        when(client.call(any(), any(), any())).thenThrow(new RetryableApiException("upstream 5xx"));

        assertThatThrownBy(() -> adapter.analyze(audioFile(), "Q", ReferenceType.GUIDE, false))
                .isInstanceOf(AudioChatFallbackRequiredException.class);
    }

    @Test
    @DisplayName("Client 가 RestClientException (네트워크 오류) 던지면 AudioChatFallbackRequiredException 으로 변환")
    void analyze_restClientException_throwsFallbackSignal() {
        when(promptBuilder.buildSystemPrompt()).thenReturn("sys");
        when(promptBuilder.buildUserPromptText(any(), any())).thenReturn("usr");
        when(client.call(any(), any(), any())).thenThrow(new ResourceAccessException("connection reset"));

        assertThatThrownBy(() -> adapter.analyze(audioFile(), "Q", ReferenceType.GUIDE, false))
                .isInstanceOf(AudioChatFallbackRequiredException.class);
    }

    @Test
    @DisplayName("Client 응답이 깨진 JSON 이면 PARSE_FAILED 가 던져진다 (parseJsonResponse 위임)")
    void analyze_malformedJson_throwsParseFailed() {
        when(promptBuilder.buildSystemPrompt()).thenReturn("sys");
        when(promptBuilder.buildUserPromptText(any(), any())).thenReturn("usr");
        when(client.call(any(), any(), any())).thenReturn("not a json");

        assertThatThrownBy(() -> adapter.analyze(audioFile(), "Q", ReferenceType.MODEL_ANSWER, false))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);
    }

    private static MultipartFile audioFile() {
        return new MockMultipartFile("audio", "answer.wav", "audio/wav", new byte[]{1, 2, 3});
    }
}
