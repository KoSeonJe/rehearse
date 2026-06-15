package com.rehearse.api.infra.ai.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.config.OpenAiResumeQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAiResumeQuestionGeneratorClient — Responses API 페이로드 조립 (system + user + PDF + strict json_schema) + 에러 매핑")
class OpenAiResumeQuestionGeneratorClientTest {

    private static final String RESPONSES_PATH = "/v1/responses";
    private static final String SUCCESS_BODY = """
            {
              "id": "resp_test",
              "model": "gpt-5.4-mini",
              "output": []
            }
            """;

    private WireMockServer wireMock;
    private OpenAiResumeQuestionGeneratorClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .http2PlainDisabled(true));
        wireMock.start();

        OpenAiResumeQuestionGeneratorProperties properties = new OpenAiResumeQuestionGeneratorProperties(
                "gpt-5.4-mini",
                120_000L,
                16_000,
                0.8,
                "http://localhost:" + wireMock.port() + RESPONSES_PATH);

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();

        client = new OpenAiResumeQuestionGeneratorClient(
                restClient,
                properties,
                new OpenAiCommonProperties("test-api-key"));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Nested
    @DisplayName("요청 페이로드")
    class RequestBody {

        @Test
        @DisplayName("system input_text + user input_text + input_file (base64 PDF) + text.format strict json_schema 로 조립한다")
        void call_buildsResponsesPayload() {
            stubSuccess();
            byte[] pdfBytes = "%PDF-1.4 dummy".getBytes();
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            String expectedDataUrl = "data:application/pdf;base64," + base64Pdf;

            client.call("you are a question generator", "OPENER_COUNT: 1", base64Pdf, sampleSchema());

            wireMock.verify(postRequestedFor(urlEqualTo(RESPONSES_PATH))
                    .withHeader("Authorization", equalTo("Bearer test-api-key"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-5.4-mini")))
                    .withRequestBody(matchingJsonPath("$.max_output_tokens", equalTo("16000")))
                    .withRequestBody(matchingJsonPath("$.input[0].role", equalTo("system")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[0].type", equalTo("input_text")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[0].text", equalTo("you are a question generator")))
                    .withRequestBody(matchingJsonPath("$.input[1].role", equalTo("user")))
                    .withRequestBody(matchingJsonPath("$.input[1].content[0].type", equalTo("input_text")))
                    .withRequestBody(matchingJsonPath("$.input[1].content[0].text", equalTo("OPENER_COUNT: 1")))
                    .withRequestBody(matchingJsonPath("$.input[1].content[1].type", equalTo("input_file")))
                    .withRequestBody(matchingJsonPath("$.input[1].content[1].filename", equalTo("resume.pdf")))
                    .withRequestBody(matchingJsonPath("$.input[1].content[1].file_data", equalTo(expectedDataUrl)))
                    .withRequestBody(matchingJsonPath("$.text.format.type", equalTo("json_schema")))
                    .withRequestBody(matchingJsonPath("$.text.format.strict", equalTo("true")))
                    .withRequestBody(matchingJsonPath("$.text.format.name", equalTo("test_schema"))));
        }

        @Test
        @DisplayName("응답 body 를 그대로 반환한다")
        void call_returnsResponseBodyAsIs() {
            stubSuccess();

            String result = client.call("prompt", "user", "base64", sampleSchema());

            assertThat(result).contains("\"id\": \"resp_test\"");
        }
    }

    @Nested
    @DisplayName("실패 흐름")
    class Failure {

        @Test
        @DisplayName("4xx (≠429) → CLIENT_ERROR")
        void call_4xx_throwsClientError() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"bad request\"}")));

            assertThatThrownBy(() -> client.call("prompt", "user", "base64", sampleSchema()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.CLIENT_ERROR);
        }

        @Test
        @DisplayName("429 → RetryableApiException")
        void call_429_throwsRetryable() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(429).withBody("{\"error\":\"rate limit\"}")));

            assertThatThrownBy(() -> client.call("prompt", "user", "base64", sampleSchema()))
                    .isInstanceOf(RetryableApiException.class);
        }

        @Test
        @DisplayName("5xx → SERVER_ERROR")
        void call_5xx_throwsServerError() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(503).withBody("upstream down")));

            assertThatThrownBy(() -> client.call("prompt", "user", "base64", sampleSchema()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.SERVER_ERROR);
        }

        @Test
        @DisplayName("응답 본문이 비어 있으면 EMPTY_RESPONSE")
        void call_emptyBody_throwsEmptyResponse() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(200).withBody("")));

            assertThatThrownBy(() -> client.call("prompt", "user", "base64", sampleSchema()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }
    }

    private void stubSuccess() {
        wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUCCESS_BODY)));
    }

    private JsonSchemaSpec sampleSchema() {
        return new JsonSchemaSpec("test_schema", Map.of(
                "type", "object",
                "required", List.of("openers", "mains"),
                "additionalProperties", false,
                "properties", Map.of("openers", Map.of(), "mains", Map.of())));
    }
}
