package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedQuestion - LLM 응답 매핑 시점 검증")
class GeneratedQuestionTest {

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("content 와 questionCategory 가 있으면 인스턴스를 생성한다")
        void should_construct_when_required_fields_present() {
            GeneratedQuestion q = new GeneratedQuestion(
                    "질문", "tts", "category", 1, "criteria", "BEHAVIORAL", "model", "strategy");
            assertThat(q.content()).isEqualTo("질문");
            assertThat(q.questionCategory()).isEqualTo("BEHAVIORAL");
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("content 가 공백이면 거절한다")
        void should_reject_when_content_blank() {
            assertThatThrownBy(() -> new GeneratedQuestion(
                    "  ", "tts", "category", 1, "criteria", "BEHAVIORAL", "model", "strategy"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("questionCategory 가 공백이면 거절한다")
        void should_reject_when_question_category_blank() {
            assertThatThrownBy(() -> new GeneratedQuestion(
                    "질문", "tts", "category", 1, "criteria", "  ", "model", "strategy"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
