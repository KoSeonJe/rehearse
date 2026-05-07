package com.rehearse.api.domain.resume.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResumeMode - WRAP_UP 제거 후 PLAYGROUND / INTERROGATION 2종만 정의된다")
class ResumeModeTest {

    @Test
    @DisplayName("enum values 가 2종 (PLAYGROUND, INTERROGATION) 만 존재한다")
    void values_containsOnlyTwoModes() {
        assertThat(ResumeMode.values())
                .containsExactly(ResumeMode.PLAYGROUND, ResumeMode.INTERROGATION);
    }

    @Test
    @DisplayName("WRAP_UP 이름은 valueOf 에서 IllegalArgumentException 을 던진다")
    void valueOf_wrapUp_throws() {
        assertThatThrownBy(() -> ResumeMode.valueOf("WRAP_UP"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RESUME_WRAP_UP 이름은 valueOf 에서 IllegalArgumentException 을 던진다")
    void valueOf_resumeWrapUp_throws() {
        assertThatThrownBy(() -> ResumeMode.valueOf("RESUME_WRAP_UP"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
