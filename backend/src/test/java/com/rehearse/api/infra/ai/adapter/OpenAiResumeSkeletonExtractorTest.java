package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAiResumeSkeletonExtractor — PDF 바이트를 OpenAI Responses API 로 전송해 Skeleton JSON 을 추출한다")
class OpenAiResumeSkeletonExtractorTest {

    private static final String RESPONSES_PATH = "/v1/responses";

    private static final String SUCCESS_RESPONSE = """
            {
              "id": "resp_test",
              "model": "gpt-4o-mini",
              "output": [
                {
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "{\\"resume_id\\":\\"r_abc12345\\",\\"candidate_level\\":\\"mid\\",\\"target_domain\\":\\"backend\\",\\"projects\\":[{\\"project_id\\":\\"p1\\",\\"project_name\\":\\"\\",\\"tech_stack\\":[\\"Spring Boot\\",\\"Redis\\"],\\"role\\":\\"백엔드 단독\\",\\"architecture\\":\\"Spring Boot + Redis\\",\\"decisions\\":[\\"Memcached vs Redis → Redis\\"]}]}"
                    }
                  ]
                }
              ]
            }
            """;

    private static final String SUCCESS_RESPONSE_WITH_FENCE = """
            {
              "id": "resp_test",
              "model": "gpt-4o-mini",
              "output": [
                {
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "```json\\n{\\"resume_id\\":\\"r_xyz98765\\",\\"candidate_level\\":\\"junior\\",\\"target_domain\\":\\"frontend\\",\\"projects\\":[]}\\n```"
                    }
                  ]
                }
              ]
            }
            """;

    private WireMockServer wireMock;
    private OpenAiResumeSkeletonExtractor extractor;

    @BeforeEach
    void setUp() throws Exception {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .dynamicPort()
                .http2PlainDisabled(true));
        wireMock.start();

        String baseUrl = "http://localhost:" + wireMock.port() + RESPONSES_PATH;
        extractor = new OpenAiResumeSkeletonExtractor(
                RestClient.builder(),
                new ObjectMapper(),
                "test-api-key",
                "gpt-4o-mini",
                60000L,
                12000,
                0.2,
                baseUrl);
        invokeInit(extractor);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Nested
    @DisplayName("정상 흐름")
    class Success {

        @Test
        @DisplayName("PDF 바이트를 base64 로 인코딩해 input_file.file_data 에 담아 전송한다")
        void extract_encodesPdfAsBase64InRequestBody() {
            stubSuccess(SUCCESS_RESPONSE);
            byte[] pdfBytes = "%PDF-1.4 dummy content".getBytes();
            String expectedDataUrl = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);

            GeneratedResumeSkeleton result = extractor.extract(pdfBytes, "hash-1");

            assertThat(result.resumeId()).isEqualTo("r_abc12345");

            wireMock.verify(postRequestedFor(urlEqualTo(RESPONSES_PATH))
                    .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-4o-mini")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[1].type", equalTo("input_file")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[1].filename", equalTo("resume.pdf")))
                    .withRequestBody(matchingJsonPath("$.input[0].content[1].file_data", equalTo(expectedDataUrl))));
        }

        @Test
        @DisplayName("system prompt 는 input_text 로 첫 번째 content 에 포함된다")
        void extract_includesSystemPromptAsInputText() {
            stubSuccess(SUCCESS_RESPONSE);

            extractor.extract("%PDF-1.4".getBytes(), "hash-2");

            wireMock.verify(postRequestedFor(urlEqualTo(RESPONSES_PATH))
                    .withRequestBody(matchingJsonPath("$.input[0].content[0].type", equalTo("input_text"))));
        }

        @Test
        @DisplayName("응답 output_text 의 JSON 을 GeneratedResumeSkeleton 으로 매핑한다")
        void extract_mapsResponseToSkeleton() {
            stubSuccess(SUCCESS_RESPONSE);

            GeneratedResumeSkeleton result = extractor.extract("%PDF-1.4".getBytes(), "hash-3");

            assertThat(result.resumeId()).isEqualTo("r_abc12345");
            assertThat(result.candidateLevel()).isEqualTo("mid");
            assertThat(result.targetDomain()).isEqualTo("backend");
            assertThat(result.projects()).hasSize(1);
            assertThat(result.projects().get(0).techStack()).containsExactly("Spring Boot", "Redis");
        }

        @Test
        @DisplayName("output_text 가 ```json fence 로 감싸여 와도 파싱한다")
        void extract_stripsJsonCodeFence() {
            stubSuccess(SUCCESS_RESPONSE_WITH_FENCE);

            GeneratedResumeSkeleton result = extractor.extract("%PDF-1.4".getBytes(), "hash-4");

            assertThat(result.resumeId()).isEqualTo("r_xyz98765");
            assertThat(result.projects()).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패 흐름")
    class Failure {

        @Test
        @DisplayName("빈 PDF 바이트 → CLIENT_ERROR")
        void extract_emptyBytes_throwsClientError() {
            assertThatThrownBy(() -> extractor.extract(new byte[0], "hash"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.CLIENT_ERROR);
        }

        @Test
        @DisplayName("OpenAI 400 응답 → CLIENT_ERROR")
        void extract_4xx_throwsClientError() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"bad request\"}")));

            assertThatThrownBy(() -> extractor.extract("%PDF-1.4".getBytes(), "hash"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.CLIENT_ERROR);
        }

        @Test
        @DisplayName("output 배열 누락 → EMPTY_RESPONSE")
        void extract_missingOutput_throwsEmptyResponse() {
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"id\":\"resp_x\",\"model\":\"gpt-4o-mini\"}")));

            assertThatThrownBy(() -> extractor.extract("%PDF-1.4".getBytes(), "hash"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }

        @Test
        @DisplayName("output_text 안의 JSON 이 깨져 있으면 PARSE_FAILED")
        void extract_malformedJsonInOutputText_throwsParseFailed() {
            String malformed = """
                    {
                      "id": "resp_x",
                      "model": "gpt-4o-mini",
                      "output": [{
                        "type": "message",
                        "content": [{"type": "output_text", "text": "not a json"}]
                      }]
                    }
                    """;
            wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(malformed)));

            assertThatThrownBy(() -> extractor.extract("%PDF-1.4".getBytes(), "hash"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.PARSE_FAILED);
        }
    }

    private void stubSuccess(String body) {
        wireMock.stubFor(post(urlEqualTo(RESPONSES_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    // @PostConstruct 은 Spring 컨텍스트 밖에서 자동 호출 안 되므로 리플렉션으로 호출.
    private static void invokeInit(OpenAiResumeSkeletonExtractor target) throws Exception {
        Method init = OpenAiResumeSkeletonExtractor.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(target);
    }
}
