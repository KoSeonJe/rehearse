package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuestionType - 단일 출처 매핑 + sub-type")
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
    }

    @Nested
    @DisplayName("STANDARD 트랙 sub-type (TECH/BEHAVIORAL × MAIN/FOLLOWUP) 매핑")
    class StandardSubTypes {

        @Test
        @DisplayName("TECH_MAIN 은 (MODEL_ANSWER, TECHNICAL) 로 매핑된다")
        void techMain_mapsToModelAnswerAndTechnical() {
            assertThat(QuestionType.TECH_MAIN.referenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(QuestionType.TECH_MAIN.feedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("TECH_FOLLOWUP 은 (MODEL_ANSWER, TECHNICAL) 로 매핑된다")
        void techFollowUp_mapsToModelAnswerAndTechnical() {
            assertThat(QuestionType.TECH_FOLLOWUP.referenceType()).isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(QuestionType.TECH_FOLLOWUP.feedbackPerspective()).isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("BEHAVIORAL_MAIN 은 (GUIDE, BEHAVIORAL) 로 매핑된다")
        void behavioralMain_mapsToGuideAndBehavioral() {
            assertThat(QuestionType.BEHAVIORAL_MAIN.referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.BEHAVIORAL_MAIN.feedbackPerspective()).isEqualTo(FeedbackPerspective.BEHAVIORAL);
        }

        @Test
        @DisplayName("BEHAVIORAL_FOLLOWUP 은 (GUIDE, BEHAVIORAL) 로 매핑된다")
        void behavioralFollowUp_mapsToGuideAndBehavioral() {
            assertThat(QuestionType.BEHAVIORAL_FOLLOWUP.referenceType()).isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.BEHAVIORAL_FOLLOWUP.feedbackPerspective()).isEqualTo(FeedbackPerspective.BEHAVIORAL);
        }
    }

    @Nested
    @DisplayName("MAIN/FOLLOWUP sentinel 은 enum 속성이 정의되지 않는다 (Phase 1.6 제거 예정)")
    class LegacySentinels {

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

    @Nested
    @DisplayName("isFollowUp / isMain / isResume helper")
    class TypeHelpers {

        @Test
        @DisplayName("isFollowUp 은 TECH_FOLLOWUP / BEHAVIORAL_FOLLOWUP / FOLLOWUP 에 대해 true")
        void isFollowUp_trueForFollowUpTypes() {
            assertThat(QuestionType.TECH_FOLLOWUP.isFollowUp()).isTrue();
            assertThat(QuestionType.BEHAVIORAL_FOLLOWUP.isFollowUp()).isTrue();
            assertThat(QuestionType.FOLLOWUP.isFollowUp()).isTrue();
        }

        @Test
        @DisplayName("isFollowUp 은 MAIN sub-type / RESUME_* 에 대해 false")
        void isFollowUp_falseForOthers() {
            assertThat(QuestionType.TECH_MAIN.isFollowUp()).isFalse();
            assertThat(QuestionType.BEHAVIORAL_MAIN.isFollowUp()).isFalse();
            assertThat(QuestionType.MAIN.isFollowUp()).isFalse();
            assertThat(QuestionType.RESUME_OPENER.isFollowUp()).isFalse();
            assertThat(QuestionType.RESUME_INTERROGATION.isFollowUp()).isFalse();
        }

        @Test
        @DisplayName("isMain 은 TECH_MAIN / BEHAVIORAL_MAIN / MAIN 에 대해 true")
        void isMain_trueForMainTypes() {
            assertThat(QuestionType.TECH_MAIN.isMain()).isTrue();
            assertThat(QuestionType.BEHAVIORAL_MAIN.isMain()).isTrue();
            assertThat(QuestionType.MAIN.isMain()).isTrue();
        }

        @Test
        @DisplayName("isResume 은 RESUME_* 3종에 대해 true, 그 외 false")
        void isResume_trueOnlyForResumeTypes() {
            assertThat(QuestionType.RESUME_OPENER.isResume()).isTrue();
            assertThat(QuestionType.RESUME_PLAYGROUND.isResume()).isTrue();
            assertThat(QuestionType.RESUME_INTERROGATION.isResume()).isTrue();
            assertThat(QuestionType.TECH_MAIN.isResume()).isFalse();
            assertThat(QuestionType.MAIN.isResume()).isFalse();
        }
    }

    @Nested
    @DisplayName("category 추론 fallback (Phase 1 한시)")
    class CategoryFallback {

        @Test
        @DisplayName("TECH_MAIN 은 category 와 무관하게 (MODEL_ANSWER, TECHNICAL) 반환")
        void techMain_ignoresCategory() {
            assertThat(QuestionType.TECH_MAIN.referenceTypeOrFallback(QuestionSetCategory.BEHAVIORAL))
                    .isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(QuestionType.TECH_MAIN.feedbackPerspectiveOrFallback(QuestionSetCategory.BEHAVIORAL))
                    .isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("MAIN sentinel + BEHAVIORAL category 는 (GUIDE, BEHAVIORAL) 폴백")
        void mainSentinel_behavioralCategory_guideBehavioralFallback() {
            assertThat(QuestionType.MAIN.referenceTypeOrFallback(QuestionSetCategory.BEHAVIORAL))
                    .isEqualTo(ReferenceType.GUIDE);
            assertThat(QuestionType.MAIN.feedbackPerspectiveOrFallback(QuestionSetCategory.BEHAVIORAL))
                    .isEqualTo(FeedbackPerspective.BEHAVIORAL);
        }

        @Test
        @DisplayName("FOLLOWUP sentinel + CS category 는 (MODEL_ANSWER, TECHNICAL) 폴백")
        void followUpSentinel_csCategory_modelAnswerTechnicalFallback() {
            assertThat(QuestionType.FOLLOWUP.referenceTypeOrFallback(QuestionSetCategory.CS_FUNDAMENTAL))
                    .isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(QuestionType.FOLLOWUP.feedbackPerspectiveOrFallback(QuestionSetCategory.CS_FUNDAMENTAL))
                    .isEqualTo(FeedbackPerspective.TECHNICAL);
        }

        @Test
        @DisplayName("MAIN sentinel + RESUME_BASED category 는 비-BEHAVIORAL 분기로 (MODEL_ANSWER, TECHNICAL) 폴백")
        void mainSentinel_resumeCategory_modelAnswerTechnicalFallback() {
            assertThat(QuestionType.MAIN.referenceTypeOrFallback(QuestionSetCategory.RESUME_BASED))
                    .isEqualTo(ReferenceType.MODEL_ANSWER);
            assertThat(QuestionType.MAIN.feedbackPerspectiveOrFallback(QuestionSetCategory.RESUME_BASED))
                    .isEqualTo(FeedbackPerspective.TECHNICAL);
        }
    }

    @Nested
    @DisplayName("RESUME_WRAP_UP 은 enum 에서 제거되었다 (FSM 2단계 단순화)")
    class WrapUpRemoval {

        @Test
        @DisplayName("RESUME_WRAP_UP 이름으로 valueOf 호출 시 IllegalArgumentException 을 던진다")
        void valueOf_resumeWrapUp_throws() {
            assertThatThrownBy(() -> QuestionType.valueOf("RESUME_WRAP_UP"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("values() 에 RESUME_OPENER / RESUME_PLAYGROUND / RESUME_INTERROGATION 만 RESUME_* 로 잔존한다")
        void values_containsOnlyThreeResumeTypes() {
            assertThat(QuestionType.values())
                    .filteredOn(t -> t.name().startsWith("RESUME_"))
                    .containsExactlyInAnyOrder(
                            QuestionType.RESUME_OPENER,
                            QuestionType.RESUME_PLAYGROUND,
                            QuestionType.RESUME_INTERROGATION);
        }
    }
}
