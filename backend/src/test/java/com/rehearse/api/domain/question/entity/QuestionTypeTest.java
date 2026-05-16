package com.rehearse.api.domain.question.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTypeTest {

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
