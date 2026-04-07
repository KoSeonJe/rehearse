package com.rehearse.api.domain.questionset.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimestampFeedbackResponseTest {

    @Test
    void parseCommentBlock_정상_JSON() {
        String json = "{\"positive\":\"좋음\",\"negative\":\"개선\",\"suggestion\":\"이렇게\"}";

        TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(json);

        assertThat(block).isNotNull();
        assertThat(block.getPositive()).isEqualTo("좋음");
        assertThat(block.getNegative()).isEqualTo("개선");
        assertThat(block.getSuggestion()).isEqualTo("이렇게");
    }

    @Test
    void parseCommentBlock_null_반환() {
        assertThat(TimestampFeedbackResponse.parseCommentBlock(null)).isNull();
        assertThat(TimestampFeedbackResponse.parseCommentBlock("")).isNull();
        assertThat(TimestampFeedbackResponse.parseCommentBlock("   ")).isNull();
    }

    @Test
    void parseCommentBlock_legacy_raw_문자열() {
        String legacy = "✓ 잘했음\n△ 보완\n→ 이렇게";

        TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(legacy);

        assertThat(block).isNotNull();
        assertThat(block.getPositive()).isEqualTo(legacy);
        assertThat(block.getNegative()).isNull();
        assertThat(block.getSuggestion()).isNull();
    }
}
