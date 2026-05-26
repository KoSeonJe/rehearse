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
}
