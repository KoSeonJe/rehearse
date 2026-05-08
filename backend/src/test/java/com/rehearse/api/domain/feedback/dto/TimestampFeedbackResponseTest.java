package com.rehearse.api.domain.feedback.dto;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
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
    @DisplayName("technicalFeedback.perspective 매핑")
    class TechnicalPerspectiveMapping {

        @Test
        @DisplayName("RESUME_PLAYGROUND 질문은 perspective=EXPERIENCE 로 노출된다")
        void perspective_EXPERIENCE_when_RESUME_PLAYGROUND() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.RESUME_PLAYGROUND)
                    .questionText("최근 경험한 프로젝트를 소개해 주세요.")
                    .orderIndex(0)
                    .build();
            TimestampFeedback feedback = createFeedback(question);
            QuestionScore score = QuestionScore.builder()
                    .questionId(1L).interviewId(1L)
                    .rubricId("resume-v1").levelFlag(null).build();
            List<QuestionScoreDimension> dims = List.of(
                    QuestionScoreDimension.builder()
                            .questionScoreId(1L)
                            .dimensionRef("experience_concreteness")
                            .score(1)
                            .observation("vague")
                            .evidenceQuote(null)
                            .build()
            );

            // when
            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, score, dims);

            // then
            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getPerspective()).isEqualTo("EXPERIENCE");
        }

        @Test
        @DisplayName("RESUME_INTERROGATION 질문은 perspective=TECHNICAL 로 노출된다")
        void perspective_TECHNICAL_when_RESUME_INTERROGATION() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.RESUME_INTERROGATION)
                    .questionText("Spring AOP 의 동작 원리를 설명해 주세요.")
                    .orderIndex(1)
                    .build();
            TimestampFeedback feedback = createFeedback(question);
            QuestionScore score = QuestionScore.builder()
                    .questionId(2L).interviewId(1L)
                    .rubricId("tech-v1").levelFlag("L1").build();
            List<QuestionScoreDimension> dims = List.of(
                    QuestionScoreDimension.builder()
                            .questionScoreId(2L)
                            .dimensionRef("clarity")
                            .score(2)
                            .observation("ok")
                            .evidenceQuote(null)
                            .build()
            );

            // when
            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, score, dims);

            // then
            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getPerspective()).isEqualTo("TECHNICAL");
        }

        @Test
        @DisplayName("Question 메타가 null 이면 perspective 는 null 로 노출된다")
        void perspective_null_when_question_null() {
            // given
            TimestampFeedback feedback = createFeedback(null);
            QuestionScore score = QuestionScore.builder()
                    .questionId(3L).interviewId(1L)
                    .rubricId("tech-v1").levelFlag(null).build();
            List<QuestionScoreDimension> dims = List.of(
                    QuestionScoreDimension.builder()
                            .questionScoreId(3L)
                            .dimensionRef("clarity")
                            .score(0)
                            .observation(null)
                            .evidenceQuote(null)
                            .build()
            );

            // when
            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, score, dims);

            // then
            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getPerspective()).isNull();
        }

        private TimestampFeedback createFeedback(Question question) {
            return TimestampFeedback.builder()
                    .question(question)
                    .startMs(0)
                    .endMs(1000)
                    .transcript("테스트 답변")
                    .isAnalyzed(true)
                    .build();
        }
    }
}
