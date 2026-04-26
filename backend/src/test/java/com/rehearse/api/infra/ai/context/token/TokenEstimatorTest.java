package com.rehearse.api.infra.ai.context.token;

import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimatorTest {

    private TokenEstimator estimator;

    @BeforeEach
    void setUp() {
        estimator = new TokenEstimator();
    }

    @Test
    @DisplayName("4자 문자열은 1토큰으로 추정된다")
    void estimate_returns1Token_when4CharString() {
        assertThat(estimator.estimate("abcd")).isEqualTo(1);
    }

    @Test
    @DisplayName("8자 문자열은 2토큰으로 추정된다")
    void estimate_returns2Tokens_when8CharString() {
        assertThat(estimator.estimate("abcdefgh")).isEqualTo(2);
    }

    @Test
    @DisplayName("null 텍스트는 0을 반환한다")
    void estimate_returns0_whenNullText() {
        assertThat(estimator.estimate((String) null)).isEqualTo(0);
    }

    @Test
    @DisplayName("빈 문자열은 0을 반환한다")
    void estimate_returns0_whenEmptyString() {
        assertThat(estimator.estimate("")).isEqualTo(0);
    }

    @Test
    @DisplayName("1자 문자열은 최소 1토큰으로 반올림된다")
    void estimate_returnsAtLeast1_whenSingleCharString() {
        assertThat(estimator.estimate("a")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("null 메시지 리스트는 0을 반환한다")
    void estimate_returns0_whenNullMessageList() {
        assertThat(estimator.estimate((List<ChatMessage>) null)).isEqualTo(0);
    }

    @Test
    @DisplayName("빈 메시지 리스트는 0을 반환한다")
    void estimate_returns0_whenEmptyMessageList() {
        assertThat(estimator.estimate(List.of())).isEqualTo(0);
    }

    @Test
    @DisplayName("메시지 리스트의 토큰 추정은 각 메시지 content 토큰의 합이다")
    void estimate_sumsMessageContents_whenMessageList() {
        List<ChatMessage> messages = List.of(
                ChatMessage.of(ChatMessage.Role.SYSTEM, "abcdefgh"),    // 2 tokens
                ChatMessage.of(ChatMessage.Role.USER, "abcdefghijkl")   // 3 tokens
        );

        assertThat(estimator.estimate(messages)).isEqualTo(5);
    }

    @Test
    @DisplayName("메시지 리스트에서 content가 빈 메시지는 0으로 계산된다")
    void estimate_treatsEmptyContentAs0_whenMessageList() {
        List<ChatMessage> messages = List.of(
                ChatMessage.of(ChatMessage.Role.SYSTEM, "abcd"),   // 1 token
                ChatMessage.of(ChatMessage.Role.USER, "")          // 0 tokens
        );

        assertThat(estimator.estimate(messages)).isEqualTo(1);
    }
}
