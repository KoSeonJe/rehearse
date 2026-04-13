package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionAnswer 엔티티")
class QuestionAnswerTest {

    @Nested
    @DisplayName("빌더 생성")
    class Builder {

        @Test
        @DisplayName("모든 필드로 QuestionAnswer를 생성하면 올바르게 설정된다")
        void build_withAllFields_createsQuestionAnswer() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("Spring AOP에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // when
            QuestionAnswer answer = QuestionAnswer.builder()
                    .question(question)
                    .startMs(1000L)
                    .endMs(5000L)
                    .build();

            // then
            assertThat(answer.getQuestion()).isSameAs(question);
            assertThat(answer.getStartMs()).isEqualTo(1000L);
            assertThat(answer.getEndMs()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("startMs와 endMs가 0이면 기본값으로 생성된다")
        void build_withZeroTimestamps_createsQuestionAnswer() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("질문입니다.")
                    .orderIndex(0)
                    .build();

            // when
            QuestionAnswer answer = QuestionAnswer.builder()
                    .question(question)
                    .startMs(0L)
                    .endMs(0L)
                    .build();

            // then
            assertThat(answer.getStartMs()).isZero();
            assertThat(answer.getEndMs()).isZero();
        }
    }
}
