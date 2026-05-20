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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("타임스탬프 피드백 응답 DTO")
class TimestampFeedbackResponseTest {

    @Test
    @DisplayName("응답 DTO가 삭제된 Lambda 자유서술 필드를 노출하지 않는다")
    void timestampResponse_doesNotExposeLambdaContent() {
        assertThat(Arrays.stream(TimestampFeedbackResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("content");

        assertThat(Arrays.stream(TimestampFeedbackResponse.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .doesNotContain("ContentFeedback", "AccuracyIssue", "CoachingResponse");
    }

    @Test
    @DisplayName("delivery/vocal/nonverbal legacy inner class 및 자유서술 필드를 응답에 노출하지 않는다")
    void response_doesNotExpose_legacy_delivery_and_freetext() {
        assertThat(Arrays.stream(TimestampFeedbackResponse.class.getDeclaredClasses())
                .map(Class::getSimpleName))
                .doesNotContain("CommentBlock", "DeliveryFeedback", "NonverbalFeedback", "VocalFeedback");

        assertThat(Arrays.stream(TimestampFeedbackResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("delivery", "overallComment");
    }

    @Nested
    @DisplayName("technicalFeedback.rubricCategory 매핑")
    class TechnicalRubricCategoryMapping {

        @Test
        @DisplayName("RESUME_OPENER 질문은 rubricCategory=EXPERIENCE 로 노출된다")
        void rubricCategory_EXPERIENCE_when_RESUME_OPENER() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_OPENER);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore score = score(100L, 1L, "resume-v1", null);
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    100L, List.of(TestFixtures.createQuestionScoreDimension(100L, "experience_concreteness", 1, "vague"))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(score), dims);

            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricCategory()).isEqualTo("EXPERIENCE");
        }

        @Test
        @DisplayName("RESUME_MAIN 질문은 rubricCategory=TECHNICAL 로 노출된다")
        void rubricCategory_TECHNICAL_when_RESUME_MAIN() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore score = score(101L, 2L, "technical-v1", "L1");
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    101L, List.of(TestFixtures.createQuestionScoreDimension(101L, "clarity", 2, "ok"))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(score), dims);

            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricCategory()).isEqualTo("TECHNICAL");
        }

        @Test
        @DisplayName("Question 메타가 null 이면 rubricCategory 는 null 로 노출된다")
        void rubricCategory_null_when_question_null() {
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(null);
            QuestionScore score = score(102L, 3L, "technical-v1", null);
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    102L, List.of(TestFixtures.createQuestionScoreDimension(102L, "clarity", 0, null))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(score), dims);

            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("verbal + nonverbal rubric 동시 노출")
    class VerbalAndNonverbalAssembly {

        @Test
        @DisplayName("verbal + nonverbal 두 rubric row 가 있으면 technicalFeedback / nonverbalFeedback 모두 적재된다")
        void builds_both_technical_and_nonverbal_when_both_question_scores_present() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore verbal = score(200L, 10L, "technical-v1", "L1");
            QuestionScore nonverbal = score(201L, 10L, "nonverbal-v1", null);
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    200L, List.of(TestFixtures.createQuestionScoreDimension(200L, "clarity", 2, "정리됨")),
                    201L, List.of(
                            TestFixtures.createQuestionScoreDimension(201L, "fluency", 2, "유창"),
                            TestFixtures.createQuestionScoreDimension(201L, "confidence_tone", 1, "톤 흔들림"),
                            TestFixtures.createQuestionScoreDimension(201L, "eye_contact_posture", 2, "안정")
                    )
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(verbal, nonverbal), dims);

            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricId()).isEqualTo("technical-v1");
            assertThat(response.getTechnicalFeedback().getDimensions()).hasSize(1);
            assertThat(response.getNonverbalFeedback()).isNotNull();
            assertThat(response.getNonverbalFeedback().rubricId()).isEqualTo("nonverbal-v1");
            assertThat(response.getNonverbalFeedback().dimensions())
                    .extracting(TimestampFeedbackResponse.TechnicalDimensionFeedback::getDimension)
                    .containsExactly("confidence_tone", "eye_contact_posture", "fluency");
        }

        @Test
        @DisplayName("nonverbal rubric row 만 있으면 nonverbalFeedback 만 적재되고 technicalFeedback 은 null 이다")
        void builds_nonverbal_only_when_verbal_absent() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore nonverbal = score(300L, 11L, "nonverbal-v1", null);
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    300L, List.of(TestFixtures.createQuestionScoreDimension(300L, "fluency", 2, "유창"))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(nonverbal), dims);

            assertThat(response.getTechnicalFeedback()).isNull();
            assertThat(response.getNonverbalFeedback()).isNotNull();
            assertThat(response.getNonverbalFeedback().rubricId()).isEqualTo("nonverbal-v1");
        }

        @Test
        @DisplayName("verbal rubric row 만 있으면 technicalFeedback 만 적재되고 nonverbalFeedback 은 null 이다")
        void builds_technical_only_when_nonverbal_absent() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore verbal = score(400L, 12L, "technical-v1", "L1");
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    400L, List.of(TestFixtures.createQuestionScoreDimension(400L, "clarity", 2, "정리됨"))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(verbal), dims);

            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getNonverbalFeedback()).isNull();
        }
    }

    @Nested
    @DisplayName("verbal rubricId 모두 technicalFeedback 매핑")
    class VerbalRubricMapping {

        @Test
        @DisplayName("resume-v1 / experience-technical-v1 / concept-cs-fundamental-v1 / concept-lang-framework-v1 / experience-collaboration-v1 / fallback-generic-v1 모두 technicalFeedback 으로 적재된다")
        void all_verbal_rubric_ids_map_to_technical_feedback() {
            String[] verbalRubricIds = {
                    "resume-v1",
                    "experience-technical-v1",
                    "concept-cs-fundamental-v1",
                    "concept-lang-framework-v1",
                    "experience-collaboration-v1",
                    "fallback-generic-v1"
            };

            long idSeed = 600L;
            for (String rubricId : verbalRubricIds) {
                Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
                TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
                QuestionScore verbal = score(idSeed, 30L, rubricId, "L1");
                Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                        idSeed, List.of(TestFixtures.createQuestionScoreDimension(idSeed, "clarity", 2, "ok"))
                );

                TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(verbal), dims);

                assertThat(response.getTechnicalFeedback())
                        .as("rubricId=%s 는 technicalFeedback 으로 적재되어야 한다", rubricId)
                        .isNotNull();
                assertThat(response.getTechnicalFeedback().getRubricId()).isEqualTo(rubricId);
                assertThat(response.getNonverbalFeedback()).isNull();
                idSeed++;
            }
        }

        @Test
        @DisplayName("nonverbal-v1 row 는 nonverbalFeedback 으로만 적재되고 technicalFeedback 은 null 이다")
        void nonverbal_rubric_id_maps_only_to_nonverbal_feedback() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore nonverbal = score(700L, 31L, "nonverbal-v1", null);
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    700L, List.of(TestFixtures.createQuestionScoreDimension(700L, "fluency", 2, "유창"))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(nonverbal), dims);

            assertThat(response.getNonverbalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback()).isNull();
        }

        @Test
        @DisplayName("미지 rubricId (legacy / 신규) 도 nonverbal-v1 이 아니면 technicalFeedback 으로 매핑된다")
        void unknown_non_nonverbal_rubric_id_maps_to_technical_feedback() {
            Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
            TimestampFeedback feedback = TestFixtures.createTimestampFeedback(question);
            QuestionScore unknown = score(800L, 32L, "future-rubric-v1", "L1");
            Map<Long, List<QuestionScoreDimension>> dims = Map.of(
                    800L, List.of(TestFixtures.createQuestionScoreDimension(800L, "clarity", 2, "ok"))
            );

            TimestampFeedbackResponse response = TimestampFeedbackResponse.from(feedback, List.of(unknown), dims);

            assertThat(response.getTechnicalFeedback()).isNotNull();
            assertThat(response.getTechnicalFeedback().getRubricId()).isEqualTo("future-rubric-v1");
            assertThat(response.getNonverbalFeedback()).isNull();
        }
    }

    private static QuestionScore score(Long id, Long questionId, String rubricId, String levelFlag) {
        QuestionScore score = QuestionScore.builder()
                .questionId(questionId)
                .interviewId(1L)
                .rubricId(rubricId)
                .levelFlag(levelFlag)
                .build();
        ReflectionTestUtils.setField(score, "id", id);
        return score;
    }
}
