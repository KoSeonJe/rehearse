package com.rehearse.api.domain.feedback.dto;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimestampFeedbackResponse - 댓글 블록 파싱")
class TimestampFeedbackResponseTest {

    @Test
    @DisplayName("plan-13 timestamp response no longer exposes Lambda content")
    void timestampResponse_doesNotExposeLambdaContent() {
        assertThat(Arrays.stream(TimestampFeedbackResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("content");

        assertThat(Arrays.stream(TimestampFeedbackResponse.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .doesNotContain("ContentFeedback", "AccuracyIssue", "CoachingResponse");
    }

    @Nested
    @DisplayName("parseCommentBlock 메서드")
    class ParseCommentBlock {

        @Test
        @DisplayName("정상적인 JSON 문자열을 CommentBlock으로 파싱한다")
        void parseCommentBlock_정상_JSON() {
            // given
            String json = "{\"positive\":\"좋음\",\"negative\":\"개선\",\"suggestion\":\"이렇게\"}";

            // when
            TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(json);

            // then
            assertThat(block).isNotNull();
            assertThat(block.getPositive()).isEqualTo("좋음");
            assertThat(block.getNegative()).isEqualTo("개선");
            assertThat(block.getSuggestion()).isEqualTo("이렇게");
        }

        @Test
        @DisplayName("null 또는 빈 문자열 입력 시 null을 반환한다")
        void parseCommentBlock_null_반환() {
            // when & then
            assertThat(TimestampFeedbackResponse.parseCommentBlock(null)).isNull();
            assertThat(TimestampFeedbackResponse.parseCommentBlock("")).isNull();
            assertThat(TimestampFeedbackResponse.parseCommentBlock("   ")).isNull();
        }

        @Test
        @DisplayName("JSON이 아닌 레거시 raw 문자열은 positive 필드에 그대로 담긴다")
        void parseCommentBlock_legacy_raw_문자열() {
            // given
            String legacy = "✓ 잘했음\n△ 보완\n→ 이렇게";

            // when
            TimestampFeedbackResponse.CommentBlock block = TimestampFeedbackResponse.parseCommentBlock(legacy);

            // then
            assertThat(block).isNotNull();
            assertThat(block.getPositive()).isEqualTo(legacy);
            assertThat(block.getNegative()).isNull();
            assertThat(block.getSuggestion()).isNull();
        }
    }

    @Nested
    @DisplayName("technicalFeedback.rubricCategory 매핑")
    class TechnicalRubricCategoryMapping {

        @Test
        @DisplayName("RESUME_PLAYGROUND 질문은 rubricCategory=EXPERIENCE 로 노출된다")
        void rubricCategory_EXPERIENCE_when_RESUME_PLAYGROUND() {
            // given
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_PLAYGROUND);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore score = TestFixtures.createQuestionScore(1L, "resume-v1", null);
            List<QuestionScoreDimension> dims = List.of(
                    TestFixtures.createQuestionScoreDimension(1L, "experience_concreteness", 1, "vague")
            );

            // when
            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, score, dims);

            // then
            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricCategory()).isEqualTo("EXPERIENCE");
        }

        @Test
        @DisplayName("RESUME_INTERROGATION 질문은 rubricCategory=TECHNICAL 로 노출된다")
        void rubricCategory_TECHNICAL_when_RESUME_INTERROGATION() {
            // given
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_INTERROGATION);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore score = TestFixtures.createQuestionScore(2L, "tech-v1", "L1");
            List<QuestionScoreDimension> dims = List.of(
                    TestFixtures.createQuestionScoreDimension(2L, "clarity", 2, "ok")
            );

            // when
            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, score, dims);

            // then
            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricCategory()).isEqualTo("TECHNICAL");
        }

        @Test
        @DisplayName("Question 메타가 null 이면 rubricCategory 는 null 로 노출된다")
        void rubricCategory_null_when_question_null() {
            // given
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(null);
            QuestionScore score = TestFixtures.createQuestionScore(3L, "tech-v1", null);
            List<QuestionScoreDimension> dims = List.of(
                    TestFixtures.createQuestionScoreDimension(3L, "clarity", 0, null)
            );

            // when
            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, score, dims);

            // then
            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricCategory()).isNull();
        }
    }
}
