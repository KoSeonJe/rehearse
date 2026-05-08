package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlockReason - 진행차단진단 reason 식별자 6종 명시 매핑")
class BlockReasonTest {

    @Test
    @DisplayName("PUBLISH_SKIP 은 publish-skip 으로 출력된다")
    void publishSkip_logValue() {
        assertThat(BlockReason.PUBLISH_SKIP.logValue()).isEqualTo("publish-skip");
    }

    @Test
    @DisplayName("QUESTION_ID_MISSING 은 questionId-missing 으로 출력된다 (카멜+케밥 혼용 보존)")
    void questionIdMissing_logValue_preservesCamelKebabMix() {
        assertThat(BlockReason.QUESTION_ID_MISSING.logValue()).isEqualTo("questionId-missing");
    }

    @Test
    @DisplayName("RESPONSE_QUESTION_ID_MISSING 은 response-questionid-missing 으로 출력된다 (운영 grep 패턴 보존)")
    void responseQuestionIdMissing_logValue() {
        assertThat(BlockReason.RESPONSE_QUESTION_ID_MISSING.logValue()).isEqualTo("response-questionid-missing");
    }

    @Test
    @DisplayName("RESPONSE_QUESTION_ID_MISMATCH 은 response-questionid-mismatch 으로 출력된다 (운영 grep 패턴 보존)")
    void responseQuestionIdMismatch_logValue() {
        assertThat(BlockReason.RESPONSE_QUESTION_ID_MISMATCH.logValue()).isEqualTo("response-questionid-mismatch");
    }

    @Test
    @DisplayName("ANALYZER_SKIP 은 analyzer-skip 으로 출력된다")
    void analyzerSkip_logValue() {
        assertThat(BlockReason.ANALYZER_SKIP.logValue()).isEqualTo("analyzer-skip");
    }

    @Test
    @DisplayName("STEP_B_SKIP 은 step-b-skip 으로 출력된다")
    void stepBSkip_logValue() {
        assertThat(BlockReason.STEP_B_SKIP.logValue()).isEqualTo("step-b-skip");
    }
}
