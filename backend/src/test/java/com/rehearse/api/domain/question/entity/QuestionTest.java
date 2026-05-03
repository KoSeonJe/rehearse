package com.rehearse.api.domain.question.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question 엔티티 - NOT NULL invariant guard")
class QuestionTest {

    @Nested
    @DisplayName("resume() 팩토리")
    class Resume {

        @Test
        @DisplayName("questionText 가 null 이면 IllegalArgumentException 을 던진다")
        void resume_nullQuestionText_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, QuestionType.RESUME_OPENER, null, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText must not be blank");
        }

        @Test
        @DisplayName("questionText 가 빈 문자열이면 IllegalArgumentException 을 던진다")
        void resume_blankQuestionText_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, QuestionType.RESUME_OPENER, "   ", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionText must not be blank");
        }

        @Test
        @DisplayName("questionType 이 null 이면 IllegalArgumentException 을 던진다")
        void resume_nullQuestionType_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, null, "유효한 질문", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("questionType must not be null");
        }

        @Test
        @DisplayName("MAIN 타입은 resume() 팩토리에서 거부된다")
        void resume_mainType_throws() {
            assertThatThrownBy(() ->
                    Question.resume(null, QuestionType.MAIN, "유효한 질문", 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RESUME_* 타입만 허용");
        }

        @Test
        @DisplayName("유효한 RESUME_OPENER 타입과 questionText 로 생성하면 성공한다")
        void resume_validArgs_createsQuestion() {
            Question q = Question.resume(null, QuestionType.RESUME_OPENER, "프로젝트를 소개해주세요.", 0);

            assertThat(q.getQuestionText()).isEqualTo("프로젝트를 소개해주세요.");
            assertThat(q.getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);
            assertThat(q.getOrderIndex()).isZero();
        }
    }

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
