package com.rehearse.api.infra.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudePromptBuilderTest {

    @ParameterizedTest
    @DisplayName("면접 시간 기반 질문 수 계산")
    @CsvSource({
            "6, 1, 2",    // 6/3=2
            "9, 1, 3",    // 9/3=3
            "15, 1, 5",   // 15/3=5
            "30, 1, 10",  // 30/3=10
            "60, 1, 20",  // 60/3=20
            "90, 1, 24",  // 90/3=30 → 최대 24로 제한
            "3, 1, 2",    // 3/3=1 → 최소 2로 제한
            "2, 1, 2",    // 2/3=0.67 → 1 → 최소 2로 제한
    })
    void calculateQuestionCount_withDuration(int durationMinutes, int typeCount, int expected) {
        int result = ClaudePromptBuilder.calculateQuestionCount(durationMinutes, typeCount);
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("유형 수 기반 질문 수 계산 (시간 미설정)")
    @CsvSource({
            "1, 5",  // 단일 유형 → 5개
            "2, 6",  // 2개 유형 → 6개
            "3, 8",  // 3개 이상 → 8개
            "5, 8",  // 5개 유형도 → 8개
    })
    void calculateQuestionCount_withoutDuration(int typeCount, int expected) {
        int result = ClaudePromptBuilder.calculateQuestionCount(null, typeCount);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("3분 단위 경계값 반올림 검증")
    void calculateQuestionCount_roundingBoundary() {
        // 7분 → 7/3=2.33 → 반올림 2
        assertThat(ClaudePromptBuilder.calculateQuestionCount(7, 1)).isEqualTo(2);
        // 8분 → 8/3=2.67 → 반올림 3
        assertThat(ClaudePromptBuilder.calculateQuestionCount(8, 1)).isEqualTo(3);
    }
}
