package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionType - RESUME 트랙 단일 출처 매핑")
class QuestionTypeTest {

    @Nested
    @DisplayName("RESUME_* 타입은 referenceType / feedbackPerspective 매핑이 결정성 있게 정의된다")
    class ResumeTypes {

        @Test
        @DisplayName("RESUME_OPENER 는 (GUIDE, EXPERIENCE) 로 매핑된다")
        void resumeOpener_mapsToGuideAndExperience() {
            assertThat(QuestionType.RESUME_OPENER.referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.RESUME_OPENER.feedbackPerspective()).isEqualTo(FeedbackPerspective.EXPERIENCE);
        }

        @Test
        @DisplayName("RESUME_PLAYGROUND 는 (GUIDE, EXPERIENCE) 로 매핑된다")
        void resumePlayground_mapsToGuideAndExperience() {
            assertThat(QuestionType.RESUME_PLAYGROUND.referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.RESUME_PLAYGROUND.feedbackPerspective()).isEqualTo(FeedbackPerspective.EXPERIENCE);
        }

        @Test
        @DisplayName("RESUME_INTERROGATION 는 (GUIDE, TECHNICAL) 로 매핑된다")
        void resumeInterrogation_mapsToGuideAndTechnical() {
            assertThat(QuestionType.RESUME_INTERROGATION.referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.RESUME_INTERROGATION.feedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("RESUME_WRAP_UP 는 (GUIDE, BEHAVIORAL) 로 매핑된다")
        void resumeWrapUp_mapsToGuideAndBehavioral() {
            assertThat(QuestionType.RESUME_WRAP_UP.referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.RESUME_WRAP_UP.feedbackPerspective()).isEqualTo(FeedbackPerspective.BEHAVIORAL);
        }
    }

    @Nested
    @DisplayName("STANDARD 트랙 타입은 enum 속성이 정의되지 않는다 (entity 컬럼 사용)")
    class StandardTypes {

        @Test
        @DisplayName("MAIN 은 referenceType / feedbackPerspective 둘 다 null 이다")
        void main_hasNoEnumProperties() {
            assertThat(QuestionType.MAIN.referenceType()).isNull();
            assertThat(QuestionType.MAIN.feedbackPerspective()).isNull();
        }

        @Test
        @DisplayName("FOLLOWUP 은 referenceType / feedbackPerspective 둘 다 null 이다")
        void followup_hasNoEnumProperties() {
            assertThat(QuestionType.FOLLOWUP.referenceType()).isNull();
            assertThat(QuestionType.FOLLOWUP.feedbackPerspective()).isNull();
        }
    }
}
