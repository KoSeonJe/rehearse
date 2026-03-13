package com.devlens.api.infra.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudePromptBuilderTest {

    @ParameterizedTest
    @DisplayName("면접 시간 기반 질문 수 계산")
    @CsvSource({
            "10, 1, 2",   // 10/5=2
            "15, 1, 3",   // 15/5=3
            "30, 1, 6",   // 30/5=6
            "60, 1, 12",  // 60/5=12
            "120, 1, 24", // 120/5=24 (최대)
            "150, 1, 24", // 150/5=30 → 최대 24로 제한
            "3, 1, 2",    // 3/5=0.6 → 1 → 최소 2로 제한
            "5, 1, 2",    // 5/5=1 → 최소 2로 제한 (반올림으로 1)
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
    @DisplayName("5분 단위 경계값 반올림 검증")
    void calculateQuestionCount_roundingBoundary() {
        // 12분 → 12/5=2.4 → 반올림 2
        assertThat(ClaudePromptBuilder.calculateQuestionCount(12, 1)).isEqualTo(2);
        // 13분 → 13/5=2.6 → 반올림 3
        assertThat(ClaudePromptBuilder.calculateQuestionCount(13, 1)).isEqualTo(3);
    }
}
