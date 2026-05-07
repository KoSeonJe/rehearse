package com.rehearse.api.domain.question.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question 엔티티 - NOT NULL invariant guard")
class QuestionTest {

    @Nested
    @DisplayName("builder()")
    class Builder {

        @Test
        @DisplayName("questionText 가 null 이면 IllegalArgumentException 을 던진다")
        void builder_nullQuestionText_throws() {
            assertThatThrownBy(() ->
                    Question.builder()
                            .questionType(QuestionType.MAIN)
                            .questionText(null)
                            .orderIndex(0)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText must not be blank");
        }

        @Test
        @DisplayName("questionText 가 공백이면 IllegalArgumentException 을 던진다")
        void builder_blankQuestionText_throws() {
            assertThatThrownBy(() ->
                    Question.builder()
                            .questionType(QuestionType.MAIN)
                            .questionText("")
                            .orderIndex(0)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText must not be blank");
        }

        @Test
        @DisplayName("questionType 이 null 이면 IllegalArgumentException 을 던진다")
        void builder_nullQuestionType_throws() {
            assertThatThrownBy(() ->
                    Question.builder()
                            .questionType(null)
                            .questionText("유효한 질문")
                            .orderIndex(0)
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionType must not be null");
        }
    }
}
