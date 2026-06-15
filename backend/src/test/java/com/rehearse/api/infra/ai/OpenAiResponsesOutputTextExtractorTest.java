package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAiResponsesOutputTextExtractor — OpenAI Responses 응답 JSON 에서 output_text 텍스트만 추출한다")
class OpenAiResponsesOutputTextExtractorTest {

    private final OpenAiResponsesOutputTextExtractor extractor =
            new OpenAiResponsesOutputTextExtractor(new ObjectMapper());

    @Nested
    @DisplayName("정상 흐름")
    class Success {

        @Test
        @DisplayName("output[0].content[].type==output_text 의 text 를 반환한다")
        void extract_returnsOutputText() {
            String body = """
                    {
                      "output": [{
                        "type": "message",
                        "content": [
                          {"type": "output_text", "text": "hello world"}
                        ]
                      }]
                    }
                    """;

            String result = extractor.extract(body);

            assertThat(result).isEqualTo("hello world");
        }

        @Test
        @DisplayName("output_text 가 아닌 type 은 건너뛰고 다음 part 의 output_text 를 반환한다")
        void extract_skipsNonOutputTextParts() {
            String body = """
                    {
                      "output": [{
                        "type": "message",
                        "content": [
                          {"type": "refusal", "text": "ignored"},
                          {"type": "output_text", "text": "picked"}
                        ]
                      }]
                    }
                    """;

            String result = extractor.extract(body);

            assertThat(result).isEqualTo("picked");
        }
    }

    @Nested
    @DisplayName("실패 흐름")
    class Failure {

        @Test
        @DisplayName("응답 본문이 null 이면 EMPTY_RESPONSE")
        void extract_nullBody_throwsEmptyResponse() {
            assertThatThrownBy(() -> extractor.extract(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }

        @Test
        @DisplayName("응답 본문이 빈 문자열이면 EMPTY_RESPONSE")
        void extract_blankBody_throwsEmptyResponse() {
            assertThatThrownBy(() -> extractor.extract("   "))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }

        @Test
        @DisplayName("output 배열이 누락되면 EMPTY_RESPONSE")
        void extract_missingOutput_throwsEmptyResponse() {
            String body = """
                    {"id": "resp_x", "model": "gpt-4o-mini"}
                    """;

            assertThatThrownBy(() -> extractor.extract(body))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }

        @Test
        @DisplayName("output 안에 output_text 가 하나도 없으면 EMPTY_RESPONSE")
        void extract_noOutputTextPart_throwsEmptyResponse() {
            String body = """
                    {
                      "output": [{
                        "type": "message",
                        "content": [
                          {"type": "refusal", "text": "no good"}
                        ]
                      }]
                    }
                    """;

            assertThatThrownBy(() -> extractor.extract(body))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }

        @Test
        @DisplayName("output_text 의 text 가 빈 문자열이면 EMPTY_RESPONSE")
        void extract_blankOutputText_throwsEmptyResponse() {
            String body = """
                    {
                      "output": [{
                        "type": "message",
                        "content": [
                          {"type": "output_text", "text": "   "}
                        ]
                      }]
                    }
                    """;

            assertThatThrownBy(() -> extractor.extract(body))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.EMPTY_RESPONSE);
        }

        @Test
        @DisplayName("응답 JSON 자체가 깨져 있으면 RESPONSE_INVALID")
        void extract_malformedJson_throwsResponseInvalid() {
            assertThatThrownBy(() -> extractor.extract("not a json"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(AiErrorCode.RESPONSE_INVALID);
        }
    }
}
