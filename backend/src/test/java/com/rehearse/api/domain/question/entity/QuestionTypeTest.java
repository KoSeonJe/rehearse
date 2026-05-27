package com.rehearse.api.domain.question.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeTest {

    @Nested
    @DisplayName("category() 매핑")
    class CategoryMapping {

        @Test
        @DisplayName("TECH 계열은 CONCEPT 카테고리로 분류된다")
        void tech_types_classified_as_concept() {
            assertThat(QuestionType.TECH_MAIN.category()).isEqualTo(QuestionCategory.CONCEPT);
            assertThat(QuestionType.TECH_FOLLOWUP.category()).isEqualTo(QuestionCategory.CONCEPT);
        }

        @Test
        @DisplayName("BEHAVIORAL 계열은 EXPERIENCE 카테고리로 분류된다")
        void behavioral_types_classified_as_experience() {
            assertThat(QuestionType.BEHAVIORAL_MAIN.category()).isEqualTo(QuestionCategory.EXPERIENCE);
            assertThat(QuestionType.BEHAVIORAL_FOLLOWUP.category()).isEqualTo(QuestionCategory.EXPERIENCE);
        }

        @Test
        @DisplayName("RESUME 계열(opener/main/followup)은 RESUME 카테고리로 분류된다")
        void resume_types_classified_as_resume() {
            assertThat(QuestionType.RESUME_OPENER.category()).isEqualTo(QuestionCategory.RESUME);
            assertThat(QuestionType.RESUME_MAIN.category()).isEqualTo(QuestionCategory.RESUME);
            assertThat(QuestionType.RESUME_FOLLOWUP.category()).isEqualTo(QuestionCategory.RESUME);
        }

        @Test
        @DisplayName("모든 QuestionType 값은 category 매핑을 누락 없이 갖는다")
        void every_type_has_category() {
            for (QuestionType type : QuestionType.values()) {
                assertThat(type.category()).isNotNull();
            }
        }
    }

    @Test
    void resume_opener_main_followup_classified_as_resume() {
        assertThat(QuestionType.RESUME_OPENER.isResume()).isTrue();
        assertThat(QuestionType.RESUME_MAIN.isResume()).isTrue();
        assertThat(QuestionType.RESUME_FOLLOWUP.isResume()).isTrue();
        assertThat(QuestionType.TECH_MAIN.isResume()).isFalse();
    }

    @Test
    void main_helper_includes_resume_main() {
        assertThat(QuestionType.RESUME_MAIN.isMain()).isTrue();
        assertThat(QuestionType.TECH_MAIN.isMain()).isTrue();
        assertThat(QuestionType.BEHAVIORAL_MAIN.isMain()).isTrue();
        assertThat(QuestionType.RESUME_OPENER.isMain()).isFalse();
        assertThat(QuestionType.RESUME_FOLLOWUP.isMain()).isFalse();
    }

    @Test
    void followup_helper_includes_resume_followup() {
        assertThat(QuestionType.RESUME_FOLLOWUP.isFollowUp()).isTrue();
        assertThat(QuestionType.TECH_FOLLOWUP.isFollowUp()).isTrue();
        assertThat(QuestionType.BEHAVIORAL_FOLLOWUP.isFollowUp()).isTrue();
        assertThat(QuestionType.RESUME_OPENER.isFollowUp()).isFalse();
    }

    @Test
    void resume_opener_uses_guide_reference_type() {
        assertThat(QuestionType.RESUME_OPENER.referenceType()).isEqualTo(ReferenceType.GUIDE);
        assertThat(QuestionType.RESUME_MAIN.referenceType()).isEqualTo(ReferenceType.GUIDE);
    }
}
