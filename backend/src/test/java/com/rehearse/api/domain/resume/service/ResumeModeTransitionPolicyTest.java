package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ResumeModeTransitionPolicy - PLAYGROUND→INTERROGATION 전환 정책")
class ResumeModeTransitionPolicyTest {

    private ResumeModeTransitionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ResumeModeTransitionPolicy();
        ReflectionTestUtils.setField(policy, "playgroundMaxTurns", 3);
    }

    @Nested
    @DisplayName("playground hard cap 판정")
    class PlaygroundHardCap {

        @Test
        @DisplayName("누적 턴 수가 임계값과 같으면 cap 도달로 판정한다")
        void returnTrue_when_turnCountAtThreshold() {
            assertThat(policy.isPlaygroundHardCapReached(3)).isTrue();
        }

        @Test
        @DisplayName("누적 턴 수가 임계값보다 크면 cap 도달로 판정한다")
        void returnTrue_when_turnCountAboveThreshold() {
            assertThat(policy.isPlaygroundHardCapReached(4)).isTrue();
        }

        @Test
        @DisplayName("누적 턴 수가 임계값 미만이면 cap 미도달로 판정한다")
        void returnFalse_when_turnCountBelowThreshold() {
            assertThat(policy.isPlaygroundHardCapReached(2)).isFalse();
        }
    }
}
