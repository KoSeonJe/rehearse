package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question 엔티티")
class QuestionTest {

    @Nested
    @DisplayName("빌더 생성")
    class Builder {

        @Test
        @DisplayName("필수 필드로 Question을 생성하면 모든 값이 올바르게 설정된다")
        void build_withRequiredFields_createsQuestion() {
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("Spring IoC 컨테이너에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            assertThat(question.getQuestionType()).isEqualTo(QuestionType.MAIN);
            assertThat(question.getQuestionText()).isEqualTo("Spring IoC 컨테이너에 대해 설명하세요.");
            assertThat(question.getOrderIndex()).isZero();
        }

        @Test
        @DisplayName("모든 필드로 Question을 생성하면 모든 값이 올바르게 설정된다")
        void build_withAllFields_createsQuestion() {
            QuestionPool pool = TestFixtures.createQuestionPool();

            Question question = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("꼬리 질문입니다.")
                    .ttsText("꼬리 질문 TTS 텍스트입니다.")
                    .modelAnswer("모범 답안입니다.")
                    .referenceType(ReferenceType.MODEL_ANSWER)
                    .feedbackPerspective(FeedbackPerspective.TECHNICAL)
                    .orderIndex(1)
                    .questionPool(pool)
                    .build();

            assertThat(question.getQuestionType()).isEqualTo(QuestionType.FOLLOWUP);
            assertThat(question.getTtsText()).isEqualTo("꼬리 질문 TTS 텍스트입니다.");
            assertThat(question.getModelAnswer()).isEqualTo("모범 답안입니다.");
            assertThat(question.getReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(question.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
            assertThat(question.getOrderIndex()).isEqualTo(1);
            assertThat(question.getQuestionPool()).isSameAs(pool);
        }

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
            Question question = Question.resume(null, QuestionType.RESUME_OPENER, "프로젝트를 소개해주세요.", 0);

            assertThat(question.getQuestionText()).isEqualTo("프로젝트를 소개해주세요.");
            assertThat(question.getQuestionType()).isEqualTo(QuestionType.RESUME_OPENER);
            assertThat(question.getOrderIndex()).isZero();
        }
    }

    @Nested
    @DisplayName("assignQuestionSet 메서드")
    class AssignQuestionSet {

        @Test
        @DisplayName("QuestionSet.addQuestion()을 통해 호출되면 questionSet이 설정된다")
        void assignQuestionSet_viaAddQuestion_setsQuestionSet() {
            QuestionSet questionSet = TestFixtures.createQuestionSet(TestFixtures.createInterview());
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("DI에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            questionSet.addQuestion(question);

            assertThat(question.getQuestionSet()).isSameAs(questionSet);
        }
    }
}
