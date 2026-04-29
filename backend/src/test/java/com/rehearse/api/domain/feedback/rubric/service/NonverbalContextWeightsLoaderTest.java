package com.rehearse.api.domain.feedback.rubric.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NonverbalContextWeightsLoader")
class NonverbalContextWeightsLoaderTest {

    private final NonverbalContextWeightsLoader loader = new NonverbalContextWeightsLoader();

    @Test
    @DisplayName("CS_FUNDAMENTAL은 0.8 multiplier를 적용")
    void resolve_csFundamental_returnsPointEight() {
        NonverbalContextWeight weight = loader.resolve("CS_FUNDAMENTAL", null, null, "easy");

        assertThat(weight.multiplier()).isEqualTo(0.8);
        assertThat(weight.composureEnabled()).isTrue();
    }

    @Test
    @DisplayName("BEHAVIORAL은 1.2 multiplier를 적용")
    void resolve_behavioral_returnsOnePointTwo() {
        NonverbalContextWeight weight = loader.resolve("BEHAVIORAL", null, null, "easy");

        assertThat(weight.multiplier()).isEqualTo(1.2);
        assertThat(weight.composureEnabled()).isTrue();
    }

    @Test
    @DisplayName("RESUME_BASED INTERROGATION은 1.3 multiplier를 적용")
    void resolve_resumeInterrogation_returnsOnePointThree() {
        NonverbalContextWeight weight = loader.resolve("RESUME_BASED", "RESUME_BASED", "INTERROGATION", "medium");

        assertThat(weight.multiplier()).isEqualTo(1.3);
        assertThat(weight.composureEnabled()).isTrue();
    }

    @Test
    @DisplayName("RESUME_BASED PLAYGROUND는 D14를 비활성화")
    void resolve_resumePlayground_disablesComposure() {
        NonverbalContextWeight weight = loader.resolve("RESUME_BASED", "RESUME_BASED", "PLAYGROUND", "hard");

        assertThat(weight.multiplier()).isEqualTo(0.9);
        assertThat(weight.composureEnabled()).isFalse();
    }
}
