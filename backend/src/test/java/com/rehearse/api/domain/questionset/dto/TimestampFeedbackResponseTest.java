package com.rehearse.api.domain.questionset.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimestampFeedbackResponse - 댓글 블록 파싱")
class TimestampFeedbackResponseTest {

    @Nested
    @DisplayName("parseCommentBlock 메서드")
    class ParseCommentBlock {

        @Test
        @DisplayName("정상적인 JSON 문자열을 CommentBlock으로 파싱한다")
        void parseCommentBlock_정상_JSON() {
            // given
            String json = "{\"positive\":\"좋음\",\"negative\":\"개선\",\"suggestion\":\"이렇게\"}";

            // when
            TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(json);

            // then
            assertThat(block).isNotNull();
            assertThat(block.getPositive()).isEqualTo("좋음");
            assertThat(block.getNegative()).isEqualTo("개선");
            assertThat(block.getSuggestion()).isEqualTo("이렇게");
        }

        @Test
        @DisplayName("null 또는 빈 문자열 입력 시 null을 반환한다")
        void parseCommentBlock_null_반환() {
            // when & then
            assertThat(TimestampFeedbackResponse.parseCommentBlock(null)).isNull();
            assertThat(TimestampFeedbackResponse.parseCommentBlock("")).isNull();
            assertThat(TimestampFeedbackResponse.parseCommentBlock("   ")).isNull();
        }

        @Test
        @DisplayName("JSON이 아닌 레거시 raw 문자열은 positive 필드에 그대로 담긴다")
        void parseCommentBlock_legacy_raw_문자열() {
            // given
            String legacy = "✓ 잘했음\n△ 보완\n→ 이렇게";

            // when
            TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(legacy);

            // then
            assertThat(block).isNotNull();
            assertThat(block.getPositive()).isEqualTo(legacy);
            assertThat(block.getNegative()).isNull();
            assertThat(block.getSuggestion()).isNull();
        }
    }
}
