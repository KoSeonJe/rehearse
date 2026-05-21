package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResponseParserTest {

    private AiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(new ObjectMapper());
    }

    @Nested
    @DisplayName("extractJson 메서드 — 마크다운 블록에서 JSON 추출")
    class ExtractJson {

        @Test
        @DisplayName("일반 JSON 문자열은 그대로 반환된다")
        void extractJson_plainJson_returnsAsIs() {
            // given
            String text = "{\"key\": \"value\"}";

            // when
            String result = parser.extractJson(text);

            // then
            assertThat(result).isEqualTo("{\"key\": \"value\"}");
        }

        @Test
        @DisplayName("```json 블록에서 JSON 내용만 추출된다")
        void extractJson_jsonCodeBlock_extractsContent() {
            // given
            String text = "```json\n{\"key\": \"value\"}\n```";

            // when
            String result = parser.extractJson(text);

            // then
            assertThat(result).isEqualTo("{\"key\": \"value\"}");
        }

        @Test
        @DisplayName("``` 블록(언어 없음)에서 JSON 내용만 추출된다")
        void extractJson_genericCodeBlock_extractsContent() {
            // given
            String text = "```\n{\"key\": \"value\"}\n```";

            // when
            String result = parser.extractJson(text);

            // then
            assertThat(result).isEqualTo("{\"key\": \"value\"}");
        }

        @Test
        @DisplayName("닫는 ``` 없이 잘린 경우 블록 이후 전체 텍스트를 반환한다")
        void extractJson_unclosedBlock_returnsRemaining() {
            // given
            String text = "```json\n{\"key\": \"value\"}";

            // when
            String result = parser.extractJson(text);

            // then
            assertThat(result).isEqualTo("{\"key\": \"value\"}");
        }

        @Test
        @DisplayName("앞뒤 공백이 제거된 JSON이 반환된다")
        void extractJson_whitespace_isTrimmed() {
            // given
            String text = "  {\"key\": \"value\"}  ";

            // when
            String result = parser.extractJson(text);

            // then
            assertThat(result).isEqualTo("{\"key\": \"value\"}");
        }

        @Test
        @DisplayName("```json 블록이 여러 개 있을 때 첫 번째 블록 내용만 추출된다")
        void extractJson_multipleBlocks_extractsFirst() {
            // given
            String text = "```json\n{\"first\": 1}\n```\n\n```json\n{\"second\": 2}\n```";

            // when
            String result = parser.extractJson(text);

            // then
            assertThat(result).isEqualTo("{\"first\": 1}");
        }
    }

    @Nested
    @DisplayName("parseJsonResponse 메서드 — JSON 역직렬화")
    class ParseJsonResponse {

        @Test
        @DisplayName("정상 JSON 문자열을 지정한 클래스로 역직렬화한다")
        void parseJsonResponse_validJson_deserializesCorrectly() {
            // given
            String text = "{\"name\": \"테스트\", \"count\": 5}";

            // when
            TestDto result = parser.parseJsonResponse(text, TestDto.class);

            // then
            assertThat(result.name).isEqualTo("테스트");
            assertThat(result.count).isEqualTo(5);
        }

        @Test
        @DisplayName("마크다운으로 래핑된 JSON도 역직렬화된다")
        void parseJsonResponse_markdownWrapped_deserializesCorrectly() {
            // given
            String text = "```json\n{\"name\": \"마크다운\", \"count\": 3}\n```";

            // when
            TestDto result = parser.parseJsonResponse(text, TestDto.class);

            // then
            assertThat(result.name).isEqualTo("마크다운");
            assertThat(result.count).isEqualTo(3);
        }

        @Test
        @DisplayName("유효하지 않은 JSON 문자열은 BusinessException(AI_005)을 발생시킨다")
        void parseJsonResponse_invalidJson_throwsBusinessException() {
            // given
            String text = "not a valid json";

            // when & then
            assertThatThrownBy(() -> parser.parseJsonResponse(text, TestDto.class))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("AI_005");
                    });
        }

        @Test
        @DisplayName("빈 문자열은 BusinessException(AI_005)을 발생시킨다")
        void parseJsonResponse_emptyString_throwsBusinessException() {
            // given
            String text = "";

            // when & then
            assertThatThrownBy(() -> parser.parseJsonResponse(text, TestDto.class))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("AI_005");
                    });
        }

        @Test
        @DisplayName("JSON 필드 값이 null이어도 역직렬화된다")
        void parseJsonResponse_nullField_deserializesWithNull() {
            // given
            String text = "{\"name\": null, \"count\": 0}";

            // when
            TestDto result = parser.parseJsonResponse(text, TestDto.class);

            // then
            assertThat(result.name).isNull();
            assertThat(result.count).isEqualTo(0);
        }

        @Test
        @DisplayName("Map 타입으로도 역직렬화된다 (제네릭 지원)")
        void parseJsonResponse_mapType_deserializesCorrectly() {
            // given
            String text = "{\"key\": \"value\", \"number\": 42}";

            // when
            @SuppressWarnings("unchecked")
            Map<String, Object> result = parser.parseJsonResponse(text, Map.class);

            // then
            assertThat(result).containsEntry("key", "value");
            assertThat(result).containsKey("number");
        }

        @Test
        @DisplayName("JSON 배열을 List 타입으로 역직렬화한다")
        void parseJsonResponse_listType_deserializesCorrectly() {
            // given
            String text = "[\"item1\", \"item2\", \"item3\"]";

            // when
            @SuppressWarnings("unchecked")
            List<String> result = parser.parseJsonResponse(text, List.class);

            // then
            assertThat(result).hasSize(3).containsExactly("item1", "item2", "item3");
        }
    }

    static class TestDto {
        public String name;
        public int count;
    }
}
