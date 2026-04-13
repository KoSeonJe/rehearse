package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionSetFeedback 엔티티")
class QuestionSetFeedbackTest {

    @Nested
    @DisplayName("빌더 생성")
    class Builder {

        @Test
        @DisplayName("QuestionSet과 코멘트로 피드백을 생성하면 올바르게 설정된다")
        void build_withRequiredFields_createsFeedback() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);

            // when
            QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                    .questionSet(questionSet)
                    .questionSetComment("전반적으로 좋은 답변입니다.")
                    .build();

            // then
            assertThat(feedback.getQuestionSet()).isSameAs(questionSet);
            assertThat(feedback.getQuestionSetComment()).isEqualTo("전반적으로 좋은 답변입니다.");
        }
    }

    @Nested
    @DisplayName("addTimestampFeedback 메서드")
    class AddTimestampFeedback {

        @Test
        @DisplayName("타임스탬프 피드백을 추가하면 컬렉션에 포함된다")
        void addTimestampFeedback_validFeedback_addsToCollection() {
            // given
            QuestionSetFeedback feedback = createFeedback();
            TimestampFeedback timestampFeedback = createTimestampFeedback();

            // when
            feedback.addTimestampFeedback(timestampFeedback);

            // then
            assertThat(feedback.getTimestampFeedbacks()).hasSize(1);
            assertThat(feedback.getTimestampFeedbacks().get(0)).isSameAs(timestampFeedback);
        }

        @Test
        @DisplayName("타임스탬프 피드백을 추가하면 해당 피드백의 questionSetFeedback이 자신으로 설정된다")
        void addTimestampFeedback_validFeedback_assignsFeedbackReference() {
            // given
            QuestionSetFeedback feedback = createFeedback();
            TimestampFeedback timestampFeedback = createTimestampFeedback();

            // when
            feedback.addTimestampFeedback(timestampFeedback);

            // then
            assertThat(timestampFeedback.getQuestionSetFeedback()).isSameAs(feedback);
        }

        @Test
        @DisplayName("여러 타임스탬프 피드백을 추가하면 모두 컬렉션에 포함된다")
        void addTimestampFeedback_multipleFeedbacks_allAddedToCollection() {
            // given
            QuestionSetFeedback feedback = createFeedback();
            TimestampFeedback tf1 = createTimestampFeedback();
            TimestampFeedback tf2 = TimestampFeedback.builder()
                    .startMs(5000L)
                    .endMs(10000L)
                    .isAnalyzed(false)
                    .build();

            // when
            feedback.addTimestampFeedback(tf1);
            feedback.addTimestampFeedback(tf2);

            // then
            assertThat(feedback.getTimestampFeedbacks()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getTimestampFeedbacks 메서드")
    class GetTimestampFeedbacks {

        @Test
        @DisplayName("반환된 리스트는 수정 불가능하다")
        void getTimestampFeedbacks_returnedList_isUnmodifiable() {
            // given
            QuestionSetFeedback feedback = createFeedback();

            // when & then
            assertThat(feedback.getTimestampFeedbacks()).isUnmodifiable();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private QuestionSetFeedback createFeedback() {
        Interview interview = TestFixtures.createInterview();
        QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
        return QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment("피드백 코멘트")
                .build();
    }

    private TimestampFeedback createTimestampFeedback() {
        return TimestampFeedback.builder()
                .startMs(0L)
                .endMs(5000L)
                .isAnalyzed(false)
                .build();
    }
}
