package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
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
        @DisplayName("모든 필드로 TimestampFeedback을 생성하면 올바르게 설정된다")
        void build_withAllFields_createsTimestampFeedback() {
            // given
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText("Spring AOP에 대해 설명하세요.")
                    .orderIndex(0)
                    .build();

            // when
            TimestampFeedback feedback = TimestampFeedback.builder()
                    .question(question)
                    .startMs(1000L)
                    .endMs(6000L)
                    .transcript("Spring AOP는 관점 지향 프로그래밍으로...")
                    .verbalComment("{\"comment\":\"좋은 설명입니다\"}")
                    .fillerWordCount(3)
                    .eyeContactLevel("GOOD")
                    .postureLevel("AVERAGE")
                    .expressionLabel("자신감")
                    .nonverbalComment("자세가 안정적입니다.")
                    .overallComment("전반적으로 우수한 답변입니다.")
                    .isAnalyzed(true)
                    .fillerWords("[\"음\",\"어\"]")
                    .speechPace("적절")
                    .toneConfidenceLevel("GOOD")
                    .emotionLabel("자신감")
                    .vocalComment("목소리 톤이 안정적입니다.")
                    .accuracyIssues("[{\"claim\":\"AOP는 디자인 패턴\",\"correction\":\"프로그래밍 패러다임\"}]")
                    .coachingStructure("서론-본론-결론 구조를 갖추었습니다.")
                    .coachingImprovement("구체적인 예시를 추가하면 좋겠습니다.")
                    .attitudeComment("적극적인 태도가 좋습니다.")
                    .build();

            // then
            assertThat(feedback.getQuestion()).isSameAs(question);
            assertThat(feedback.getStartMs()).isEqualTo(1000L);
            assertThat(feedback.getEndMs()).isEqualTo(6000L);
            assertThat(feedback.getTranscript()).isEqualTo("Spring AOP는 관점 지향 프로그래밍으로...");
            assertThat(feedback.getVerbalComment()).isEqualTo("{\"comment\":\"좋은 설명입니다\"}");
            assertThat(feedback.getFillerWordCount()).isEqualTo(3);
            assertThat(feedback.getEyeContactLevel()).isEqualTo("GOOD");
            assertThat(feedback.getPostureLevel()).isEqualTo("AVERAGE");
            assertThat(feedback.getExpressionLabel()).isEqualTo("자신감");
            assertThat(feedback.getNonverbalComment()).isEqualTo("자세가 안정적입니다.");
            assertThat(feedback.getOverallComment()).isEqualTo("전반적으로 우수한 답변입니다.");
            assertThat(feedback.isAnalyzed()).isTrue();
            assertThat(feedback.getFillerWords()).isEqualTo("[\"음\",\"어\"]");
            assertThat(feedback.getSpeechPace()).isEqualTo("적절");
            assertThat(feedback.getToneConfidenceLevel()).isEqualTo("GOOD");
            assertThat(feedback.getEmotionLabel()).isEqualTo("자신감");
            assertThat(feedback.getVocalComment()).isEqualTo("목소리 톤이 안정적입니다.");
            assertThat(feedback.getAccuracyIssues()).contains("AOP는 디자인 패턴");
            assertThat(feedback.getCoachingStructure()).isEqualTo("서론-본론-결론 구조를 갖추었습니다.");
            assertThat(feedback.getCoachingImprovement()).isEqualTo("구체적인 예시를 추가하면 좋겠습니다.");
            assertThat(feedback.getAttitudeComment()).isEqualTo("적극적인 태도가 좋습니다.");
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
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
    }
}
