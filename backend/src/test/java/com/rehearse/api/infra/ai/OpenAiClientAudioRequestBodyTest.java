package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * gpt-4o-*-audio-preview 모델은 response_format=json_object 를 거부한다 (HTTP 400).
 * audio chat 호출 body 에 response_format 이 절대 포함되지 않아야 한다.
 */
class OpenAiClientAudioRequestBodyTest {

    private static ChatRequest jsonObjectRequest() {
        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "test")))
                .callType("audio_turn_analyzer")
                .temperature(0.2)
                .maxTokens(1024)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();
    }

    @Test
    @DisplayName("buildAudioRequestBody 는 response_format 키를 포함하지 않는다 (audio model 미지원)")
    void buildAudioRequestBody_omitsResponseFormat() {
        Map<String, Object> body = OpenAiClient.buildAudioRequestBody(
                "gpt-4o-mini-audio-preview",
                List.of(Map.of("role", "user", "content", "test")),
                1024,
                jsonObjectRequest()
        );

        assertThat(body).doesNotContainKey("response_format");
        assertThat(body).containsEntry("model", "gpt-4o-mini-audio-preview");
        assertThat(body).containsEntry("max_tokens", 1024);
        assertThat(body).containsEntry("temperature", 0.2);
    }

    @Test
    @DisplayName("buildRequestBody (text chat) 은 JSON_OBJECT 요청 시 response_format 을 포함한다")
    void buildRequestBody_includesResponseFormat() {
        Map<String, Object> body = OpenAiClient.buildRequestBody(
                "gpt-4o-mini",
                List.of(Map.of("role", "user", "content", "test")),
                800,
                jsonObjectRequest()
        );

        assertThat(body).containsKey("response_format");
        Object rf = body.get("response_format");
        assertThat(rf).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> rfMap = (Map<String, Object>) rf;
        assertThat(rfMap).containsEntry("type", "json_object");
    }
}
