package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question 엔티티")
class QuestionTest {

    @Nested
    @DisplayName("빌더 생성")
    class Builder {

        @Test
        @DisplayName("필수 필드로 Question을 생성하면 모든 값이 올바르게 설정된다")
        void build_withRequiredFields_createsQuestion() {
            // given & when
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("Spring IoC 컨테이너에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // then
            assertThat(question.getQuestionType()).isEqualTo(QuestionType.MAIN);
            assertThat(question.getQuestionText()).isEqualTo("Spring IoC 컨테이너에 대해 설명하세요.");
            assertThat(question.getOrderIndex()).isZero();
        }

        @Test
        @DisplayName("모든 필드로 Question을 생성하면 모든 값이 올바르게 설정된다")
        void build_withAllFields_createsQuestion() {
            // given
            QuestionPool pool = TestFixtures.createQuestionPool();

            // when
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

            // then
            assertThat(question.getQuestionType()).isEqualTo(QuestionType.FOLLOWUP);
            assertThat(question.getTtsText()).isEqualTo("꼬리 질문 TTS 텍스트입니다.");
            assertThat(question.getModelAnswer()).isEqualTo("모범 답안입니다.");
            assertThat(question.getReferenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(question.getFeedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
            assertThat(question.getOrderIndex()).isEqualTo(1);
            assertThat(question.getQuestionPool()).isSameAs(pool);
        }
    }

    @Nested
    @DisplayName("assignQuestionSet 메서드")
    class AssignQuestionSet {

        @Test
        @DisplayName("QuestionSet.addQuestion()을 통해 호출되면 questionSet이 설정된다")
        void assignQuestionSet_viaAddQuestion_setsQuestionSet() {
            // given
            QuestionSet questionSet = TestFixtures.createQuestionSet(TestFixtures.createInterview());
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("DI에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // when
            questionSet.addQuestion(question);

            // then
            assertThat(question.getQuestionSet()).isSameAs(questionSet);
        }
    }
}
