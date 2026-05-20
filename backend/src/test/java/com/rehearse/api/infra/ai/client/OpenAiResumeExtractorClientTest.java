package com.rehearse.api.infra.ai.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.properties.OpenAiResumeSkeletonProperties;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.exception.RetryableApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAiResumeExtractorClient — Responses API 요청 페이로드 조립 + HTTP 호출 + 에러 매핑")
class OpenAiResumeExtractorClientTest {

    private static final String RESPONSES_PATH = "/v1/responses";
    private static final String SUCCESS_BODY = """
            {
              "id": "resp_test",
              "model": "gpt-4o-mini",
              "output": []
            }
            """;

    private WireMockServer wireMock;
    private OpenAiResumeExtractorClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .http2PlainDisabled(true));
        wireMock.start();

        OpenAiResumeSkeletonProperties properties = new OpenAiResumeSkeletonProperties(
                "gpt-4o-mini",
                60_000L,
                12_000,
                0.2,
                "http://localhost:" + wireMock.port() + RESPONSES_PATH);

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();

        client = new OpenAiResumeExtractorClient(
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
        @DisplayName("system prompt 는 input_text 로, base64 PDF 는 input_file.file_data 로 직렬화한다")
        void call_buildsResponsesPayload() {
            stubSuccess();
            byte[] pdfBytes = "%PDF-1.4 dummy content".getBytes();
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            String expectedDataUrl = "data:application/pdf;base64," + base64Pdf;

            client.call("you are a resume parser", base64Pdf);

            wireMock.verify(postRequestedFor(urlEqualTo(RESPONSES_PATH))
                    .withHeader("Authorization", equalTo("Bearer test-api-key"))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-4o-mini")))
                    .withRequestBody(matchingJsonPath("$.max_output_tokens", equalTo("12000")))
                    .withRequestBody(matchingJsonPath("$.input[0].role", equalTo("user")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[0].type", equalTo("input_text")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[0].text", equalTo("you are a resume parser")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[1].type", equalTo("input_file")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[1].filename", equalTo("resume.pdf")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[1].file_data", equalTo(expectedDataUrl))));
        }

        @Test
        @DisplayName("응답 body 를 그대로 반환한다")
        void call_returnsResponseBodyAsIs() {
            stubSuccess();

            String result = client.call("prompt", "base64");

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

            assertThatThrownBy(() -> client.call("prompt", "base64"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.CLIENT_ERROR);
        }

        @Test
        @DisplayName("429 → RetryableApiException")
        void call_429_throwsRetryable() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(429).withBody("{\"error\":\"rate limit\"}")));

            assertThatThrownBy(() -> client.call("prompt", "base64"))
                    .isInstanceOf(RetryableApiException.class);
        }

        @Test
        @DisplayName("5xx → SERVER_ERROR")
        void call_5xx_throwsServerError() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(503).withBody("upstream down")));

            assertThatThrownBy(() -> client.call("prompt", "base64"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.SERVER_ERROR);
        }

        @Test
        @DisplayName("응답 본문이 비어 있으면 EMPTY_RESPONSE")
        void call_emptyBody_throwsEmptyResponse() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(200).withBody("")));

            assertThatThrownBy(() -> client.call("prompt", "base64"))
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
}
