package com.rehearse.api.infra.ai;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAiClient — modelOverride POST body 검증 (plan 검증 #5).
 *
 * <p>WireMock 으로 OpenAI API 를 모킹하고, ChatRequest.modelOverride="gpt-4o" 설정 시
 * 실제 HTTP POST body 의 "model" 필드가 "gpt-4o" 로 전송되는지 검증한다.</p>
 */
@DisplayName("OpenAiClient — modelOverride 가 HTTP body 의 model 필드에 반영된다")
class OpenAiChatModelOverrideTest {

    private WireMockServer wireMock;
    private OpenAiClient openAiClient;

    private static final String STUB_RESPONSE = """
            {
              "id": "test-id",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "테스트 응답"},
                "finish_reason": "stop"
              }],
              "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
            }
            """;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .http2PlainDisabled(true));
        wireMock.start();

        String baseUrl = "http://localhost:" + wireMock.port() + "/v1/chat/completions";

        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STUB_RESPONSE)));

        // AiResponseParser 는 실제 ObjectMapper 사용
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        AiResponseParser parser = new AiResponseParser(
                objectMapper,
                new SchemaExampleRegistry(),
                org.mockito.Mockito.mock(com.rehearse.api.infra.ai.metrics.AiCallMetrics.class));

        // PromptBuilders: 이 테스트에서는 직접 ChatRequest.chat() 을 호출하므로 mock 불필요
        com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder qBuilder =
                org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder.class);
        com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder fBuilder =
                org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder.class);

        openAiClient = new OpenAiClient(
                RestClient.builder(),
                qBuilder,
                fBuilder,
                parser,
                "test-api-key",
                "gpt-4o-mini",  // 기본 모델
                "gpt-4o-mini-audio-preview",  // audio 모델
                baseUrl
        );
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("modelOverride='gpt-4o' 설정 시 POST body 의 model 필드가 'gpt-4o' 이다")
    void chat_modelOverride_sentInRequestBody() {
        // given
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.of(ChatMessage.Role.USER, "테스트 질문")
                ))
                .modelOverride("gpt-4o")
                .callType("test_override")
                .build();

        // when
        ChatResponse response = openAiClient.chat(request);

        // then — 응답 검증
        assertThat(response.content()).isEqualTo("테스트 응답");
        assertThat(response.model()).isEqualTo("gpt-4o");

        // WireMock body 검증 — "model": "gpt-4o" 가 전송됐는지
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-4o"))));
    }

    @Test
    @DisplayName("modelOverride 없을 때 기본 모델 'gpt-4o-mini' 가 사용된다")
    void chat_noModelOverride_usesDefaultModel() {
        // given
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.of(ChatMessage.Role.USER, "테스트 질문")
                ))
                .callType("test_default")
                .build();

        // when
        ChatResponse response = openAiClient.chat(request);

        // then
        assertThat(response.model()).isEqualTo("gpt-4o-mini");

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-4o-mini"))));
    }

    @Test
    @DisplayName("usage 정보가 ChatResponse.Usage 에 매핑된다")
    void chat_usageFieldsMapped() {
        // given
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "질문")))
                .callType("test_usage")
                .build();

        // when
        ChatResponse response = openAiClient.chat(request);

        // then
        assertThat(response.usage().inputTokens()).isEqualTo(10);
        assertThat(response.usage().outputTokens()).isEqualTo(5);
    }
}
