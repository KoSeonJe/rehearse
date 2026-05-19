package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.OpenAiResponsesOutputTextExtractor;
import com.rehearse.api.infra.ai.client.OpenAiResumeExtractorClient;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiResumeSkeletonExtractor — PDF 바이트를 base64 로 인코딩하고 Client 응답을 Skeleton 으로 매핑한다")
class OpenAiResumeSkeletonExtractorTest {

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

    private OpenAiResumeExtractorClient client;
    private OpenAiResumeSkeletonExtractor extractor;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        OpenAiResponsesOutputTextExtractor outputTextExtractor =
                new OpenAiResponsesOutputTextExtractor(objectMapper);
        AiResponseParser aiResponseParser = new AiResponseParser(objectMapper, null, null);
        client = mock(OpenAiResumeExtractorClient.class);

        extractor = new OpenAiResumeSkeletonExtractor(client, outputTextExtractor, aiResponseParser);
        invokeInit(extractor);
    }

    @Nested
    @DisplayName("정상 흐름")
    class Success {

        @Test
        @DisplayName("PDF 바이트를 base64 로 인코딩해 Client.call 의 두 번째 인자로 넘긴다")
        void extract_passesBase64EncodedPdfToClient() {
            byte[] pdfBytes = "%PDF-1.4 dummy content".getBytes();
            String expectedBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            when(client.call(any(), eq(expectedBase64))).thenReturn(SUCCESS_RESPONSE);

            extractor.extract(pdfBytes, "hash-1");

            verify(client).call(any(), eq(expectedBase64));
        }

        @Test
        @DisplayName("classpath 의 resume-extractor.txt 프롬프트를 Client.call 첫 인자로 넘긴다")
        void extract_passesSystemPromptToClient() {
            when(client.call(any(), any())).thenReturn(SUCCESS_RESPONSE);

            extractor.extract("%PDF-1.4".getBytes(), "hash-2");

            verify(client).call(org.mockito.ArgumentMatchers.argThat(
                    prompt -> prompt != null && !prompt.isBlank()), any());
        }

        @Test
        @DisplayName("Client 응답의 output_text JSON 을 GeneratedResumeSkeleton 으로 매핑한다")
        void extract_mapsResponseToSkeleton() {
            when(client.call(any(), any())).thenReturn(SUCCESS_RESPONSE);

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
            when(client.call(any(), any())).thenReturn(SUCCESS_RESPONSE_WITH_FENCE);

            GeneratedResumeSkeleton result = extractor.extract("%PDF-1.4".getBytes(), "hash-4");

            assertThat(result.resumeId()).isEqualTo("r_xyz98765");
            assertThat(result.projects()).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패 흐름")
    class Failure {

        @Test
        @DisplayName("빈 PDF 바이트 → CLIENT_ERROR (Client 호출 안 함)")
        void extract_emptyBytes_throwsClientError() {
            assertThatThrownBy(() -> extractor.extract(new byte[0], "hash"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.CLIENT_ERROR);
        }

        @Test
        @DisplayName("output 배열 누락 응답 → EMPTY_RESPONSE")
        void extract_missingOutput_throwsEmptyResponse() {
            when(client.call(any(), any()))
                    .thenReturn("{\"id\":\"resp_x\",\"model\":\"gpt-4o-mini\"}");

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
            when(client.call(any(), any())).thenReturn(malformed);

            assertThatThrownBy(() -> extractor.extract("%PDF-1.4".getBytes(), "hash"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.PARSE_FAILED);
        }
    }

    private static void invokeInit(OpenAiResumeSkeletonExtractor target) throws Exception {
        Method init = OpenAiResumeSkeletonExtractor.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(target);
    }
}
