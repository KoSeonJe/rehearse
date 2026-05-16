package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedFollowUpTest {

    @Test
    void skipped_factory_marks_skip_true() {
        GeneratedFollowUp skipped = GeneratedFollowUp.skipped("all_gaps_low");
        assertThat(skipped.isSkipped()).isTrue();
        assertThat(skipped.skipReason()).isEqualTo("all_gaps_low");
        assertThat(skipped.question()).isNull();
    }

    @Test
    void non_skip_requires_question() {
        assertThatThrownBy(() -> new GeneratedFollowUp(
                false, null, "  ", "tts", "reason", "DEEP_DIVE", "best", "answer", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skip_allows_null_question() {
        GeneratedFollowUp fu = new GeneratedFollowUp(
                true, "step_b_skip", null, null, null, null, null, null, null);
        assertThat(fu.isSkipped()).isTrue();
    }

    @Test
    void with_answer_text_returns_copy_with_new_text() {
        GeneratedFollowUp original = new GeneratedFollowUp(
                false, null, "Q?", "tts", "reason", "DEEP_DIVE", "best", "old", 0);
        GeneratedFollowUp updated = original.withAnswerText("new");
        assertThat(updated.answerText()).isEqualTo("new");
        assertThat(original.answerText()).isEqualTo("old");
    }
}
