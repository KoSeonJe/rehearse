package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClaudeApiClient — JSON_SCHEMA 요청 시 system 메시지에 schema JSON 이 prepend 되는지 검증.
 *
 * <p>Claude API 는 response_format 파라미터 미지원이므로, jsonSchema 가 있을 때
 * system 메시지 본문에 직렬화된 schema 를 포함시켜 모델이 스키마를 따르도록 유도한다.</p>
 */
@DisplayName("ClaudeApiClient — JSON_SCHEMA 시 system 에 schema JSON prepend")
class ClaudeApiClientJsonSchemaTest {

    private WireMockServer wireMock;
    private ClaudeApiClient claudeApiClient;

    private static final String STUB_RESPONSE = """
            {
              "id": "msg_test",
              "type": "message",
              "role": "assistant",
              "model": "claude-sonnet-4-20250514",
              "content": [{"type": "text", "text": "{\\"ok\\": true}"}],
              "stop_reason": "end_turn",
              "usage": {
                "input_tokens": 10,
                "output_tokens": 5,
                "cache_creation_input_tokens": 0,
                "cache_read_input_tokens": 0
              }
            }
            """;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .http2PlainDisabled(true));
        wireMock.start();

        String baseUrl = "http://localhost:" + wireMock.port() + "/v1/messages";

        wireMock.stubFor(post(urlEqualTo("/v1/messages"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(STUB_RESPONSE)));

        ObjectMapper objectMapper = new ObjectMapper();
        AiResponseParser parser = new AiResponseParser(
                objectMapper,
                org.mockito.Mockito.mock(AiCallMetrics.class));

        com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder qBuilder =
                org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder.class);
        com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder fBuilder =
                org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder.class);

        claudeApiClient = new ClaudeApiClient(
                RestClient.builder(),
                qBuilder,
                fBuilder,
                parser,
                objectMapper,
                "test-api-key",
                "claude-sonnet-4-20250514",
                baseUrl
        );
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("JSON_SCHEMA 요청 시 system 첫 항목에 schema JSON 이 포함된다")
    void chat_jsonSchema_prependsSchemaJsonToSystem() {
        Map<String, Object> schemaBody = Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("ok"),
                "properties", Map.of("ok", Map.of("type", "boolean"))
        );
        JsonSchemaSpec spec = new JsonSchemaSpec("test_schema", schemaBody);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "테스트")))
                .responseFormat(ResponseFormat.JSON_SCHEMA)
                .jsonSchema(spec)
                .callType("test_schema_call")
                .build();

        ChatResponse response = claudeApiClient.chat(request);

        assertThat(response.content()).contains("ok");

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.system[0].text",
                        containing("JSON Schema"))));
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.system[0].text",
                        containing("\"additionalProperties\":false"))));
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.system[0].text",
                        containing("\"required\":[\"ok\"]"))));
    }

    @Test
    @DisplayName("JSON_OBJECT 요청 시 기존 JSON_OBJECT_INSTRUCTION 만 적용되고 schema JSON 미포함")
    void chat_jsonObject_doesNotIncludeSchemaJson() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "테스트")))
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType("test_object_call")
                .build();

        claudeApiClient.chat(request);

        wireMock.verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.system[0].text",
                        containing("single JSON object"))));
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/messages"))
                .withRequestBody(matchingJsonPath("$.system[0].text",
                        notMatching(".*JSON Schema.*"))));
    }
}
