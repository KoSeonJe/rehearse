package com.rehearse.api.infra.ai.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionCountCalculatorTest {

    @ParameterizedTest
    @DisplayName("면접 시간 기반 질문 수 계산")
    @CsvSource({
            "6, 1, 2",
            "9, 1, 3",
            "15, 1, 5",
            "30, 1, 10",
            "60, 1, 20",
            "90, 1, 24",
            "3, 1, 2",
            "2, 1, 2",
    })
    void calculate_withDuration(int durationMinutes, int typeCount, int expected) {
        assertThat(QuestionCountCalculator.calculate(durationMinutes, typeCount)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("유형 수 기반 질문 수 계산 (시간 미설정)")
    @CsvSource({
            "1, 5",
            "2, 6",
            "3, 8",
            "5, 8",
    })
    void calculate_withoutDuration(int typeCount, int expected) {
        assertThat(QuestionCountCalculator.calculate(null, typeCount)).isEqualTo(expected);
    }

    @Test
    @DisplayName("3분 단위 경계값 반올림 검증")
    void calculate_roundingBoundary() {
        assertThat(QuestionCountCalculator.calculate(7, 1)).isEqualTo(2);
        assertThat(QuestionCountCalculator.calculate(8, 1)).isEqualTo(3);
    }
}
