package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedFollowUp - LLM 응답 매핑 시점 검증 (skip 분기 보존)")
class GeneratedFollowUpTest {

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("skip=false 이고 question 이 있으면 인스턴스를 생성한다")
        void should_construct_when_not_skipped_and_question_present() {
            GeneratedFollowUp g = new GeneratedFollowUp(
                    false, null, "후속 질문", "tts", "reason", "DEEP_DIVE",
                    "model", "answer", 0, "TRADEOFF");
            assertThat(g.isSkipped()).isFalse();
            assertThat(g.question()).isEqualTo("후속 질문");
        }

        @Test
        @DisplayName("skip=true 이면 question 이 null 이어도 통과한다")
        void should_allow_null_question_when_skipped() {
            GeneratedFollowUp g = new GeneratedFollowUp(
                    true, "답변 부족", null, null, null, null,
                    null, "answer", null, null);
            assertThat(g.isSkipped()).isTrue();
        }

        @Test
        @DisplayName("withAnswerText 는 answerText 만 교체한 새 인스턴스를 반환한다")
        void should_return_new_instance_with_replaced_answer_text() {
            GeneratedFollowUp original = new GeneratedFollowUp(
                    false, null, "Q", "tts", "r", "DEEP_DIVE",
                    "m", "old", 0, null);

            GeneratedFollowUp updated = original.withAnswerText("new");

            assertThat(updated.answerText()).isEqualTo("new");
            assertThat(updated.question()).isEqualTo("Q");
            assertThat(original.answerText()).isEqualTo("old");
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("skip=false 인데 question 이 null 이면 거절한다")
        void should_reject_when_not_skipped_and_question_null() {
            assertThatThrownBy(() -> new GeneratedFollowUp(
                    false, null, null, null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("skip=false 인데 question 이 공백이면 거절한다")
        void should_reject_when_not_skipped_and_question_blank() {
            assertThatThrownBy(() -> new GeneratedFollowUp(
                    false, null, "  ", null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
