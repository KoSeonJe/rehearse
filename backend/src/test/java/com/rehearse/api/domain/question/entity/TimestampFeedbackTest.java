package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.interview.entity.InterviewType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimestampFeedback 엔티티")
class TimestampFeedbackTest {

    @Nested
    @DisplayName("빌더 생성")
    class Builder {

        @Test
        @DisplayName("필수 필드로 TimestampFeedback을 생성하면 올바르게 설정된다")
        void build_withRequiredFields_createsTimestampFeedback() {
            // given & when
            TimestampFeedback feedback = TimestampFeedback.builder()
                    .startMs(0L)
                    .endMs(5000L)
                    .isAnalyzed(false)
                    .build();

            // then
            assertThat(feedback.getStartMs()).isZero();
            assertThat(feedback.getEndMs()).isEqualTo(5000L);
            assertThat(feedback.isAnalyzed()).isFalse();
        }

        @Test
        @DisplayName("잔존 필드 (transcript / filler / analyzed) 만으로 TimestampFeedback 을 생성하면 올바르게 설정된다")
        void build_withAllFields_createsTimestampFeedback() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.TECH_MAIN)
                    .questionText("Spring AOP에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // when
            TimestampFeedback feedback = TimestampFeedback.builder()
                    .question(question)
                    .startMs(1000L)
                    .endMs(6000L)
                    .transcript("Spring AOP는 관점 지향 프로그래밍으로...")
                    .fillerWordCount(3)
                    .isAnalyzed(true)
                    .fillerWords("[\"음\",\"어\"]")
                    .build();

            // then
            assertThat(feedback.getQuestion()).isSameAs(question);
            assertThat(feedback.getStartMs()).isEqualTo(1000L);
            assertThat(feedback.getEndMs()).isEqualTo(6000L);
            assertThat(feedback.getTranscript()).isEqualTo("Spring AOP는 관점 지향 프로그래밍으로...");
            assertThat(feedback.getFillerWordCount()).isEqualTo(3);
            assertThat(feedback.isAnalyzed()).isTrue();
            assertThat(feedback.getFillerWords()).isEqualTo("[\"음\",\"어\"]");
        }
    }

    @Nested
    @DisplayName("assignQuestionSetFeedback 메서드")
    class AssignQuestionSetFeedback {

        @Test
        @DisplayName("QuestionSetFeedback.addTimestampFeedback()을 통해 호출되면 참조가 설정된다")
        void assignQuestionSetFeedback_viaAddTimestampFeedback_setsFeedbackReference() {
            // given
            QuestionSetFeedback qsFeedback = QuestionSetFeedback.builder()
                    .questionSet(createQuestionSet())
                    .questionSetComment("코멘트")
                    .build();
            TimestampFeedback timestampFeedback = TimestampFeedback.builder()
                    .startMs(0L)
                    .endMs(5000L)
                    .isAnalyzed(false)
                    .build();

            // when
            qsFeedback.addTimestampFeedback(timestampFeedback);

            // then
            assertThat(timestampFeedback.getQuestionSetFeedback()).isSameAs(qsFeedback);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private QuestionSet createQuestionSet() {
        return QuestionSet.builder()
                .interview(com.rehearse.api.global.support.TestFixtures.createInterview())
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
    }
}
