package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OffTopicEscalationDetector - 연속 OFF_TOPIC 감지")
class OffTopicEscalationDetectorTest {

    private final OffTopicEscalationDetector detector = new OffTopicEscalationDetector();
    private static final String MARKER = OffTopicResponseHandler.OFF_TOPIC_CONNECTOR;
    private static final String MAIN_Q = "HashMap 충돌을 설명해주세요.";

    private FollowUpExchange offTopicExchange() {
        return new FollowUpExchange("리드인 " + MARKER + " " + MAIN_Q, "잡담");
    }

    private FollowUpExchange normalExchange() {
        return new FollowUpExchange("정상 꼬리 질문", "기술 답변");
    }

    @Nested
    @DisplayName("countRecentConsecutive - 연속 OFF_TOPIC 개수")
    class CountRecentConsecutive {

        @Test
        @DisplayName("previousExchanges가 null이면 0을 반환한다")
        void count_nullExchanges_returnsZero() {
            assertThat(detector.countRecentConsecutive(null)).isEqualTo(0);
        }

        @Test
        @DisplayName("previousExchanges가 빈 리스트이면 0을 반환한다")
        void count_emptyExchanges_returnsZero() {
            assertThat(detector.countRecentConsecutive(List.of())).isEqualTo(0);
        }

        @Test
        @DisplayName("OFF_TOPIC 마커가 없는 교환이면 0을 반환한다")
        void count_noOffTopicMarker_returnsZero() {
            List<FollowUpExchange> exchanges = List.of(normalExchange(), normalExchange());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(0);
        }

        @Test
        @DisplayName("마지막 1개가 OFF_TOPIC이면 1을 반환한다")
        void count_oneConsecutiveOffTopic_returnsOne() {
            List<FollowUpExchange> exchanges = List.of(normalExchange(), offTopicExchange());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(1);
        }

        @Test
        @DisplayName("마지막 2개가 연속 OFF_TOPIC이면 2를 반환한다")
        void count_twoConsecutiveOffTopic_returnsTwo() {
            List<FollowUpExchange> exchanges = List.of(normalExchange(), offTopicExchange(), offTopicExchange());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(2);
        }

        @Test
        @DisplayName("마지막 3개가 연속 OFF_TOPIC이면 3을 반환한다")
        void count_threeConsecutiveOffTopic_returnsThree() {
            List<FollowUpExchange> exchanges = List.of(offTopicExchange(), offTopicExchange(), offTopicExchange());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(3);
        }

        @Test
        @DisplayName("중간에 정상 교환이 끼어들면 연속 카운트가 리셋된다")
        void count_normalExchangeInterrupts_resetsCount() {
            List<FollowUpExchange> exchanges = List.of(
                    offTopicExchange(), offTopicExchange(), normalExchange(), offTopicExchange());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("shouldEscalate - 임계값 도달 여부")
    class ShouldEscalate {

        @Test
        @DisplayName("consecutive + 1이 limit 미만이면 false를 반환한다")
        void shouldEscalate_belowLimit_returnsFalse() {
            assertThat(detector.shouldEscalate(1, 3)).isFalse();
        }

        @Test
        @DisplayName("consecutive + 1이 limit과 같으면 true를 반환한다")
        void shouldEscalate_exactlyAtLimit_returnsTrue() {
            assertThat(detector.shouldEscalate(2, 3)).isTrue();
        }

        @Test
        @DisplayName("consecutive + 1이 limit을 초과해도 true를 반환한다")
        void shouldEscalate_aboveLimit_returnsTrue() {
            assertThat(detector.shouldEscalate(3, 3)).isTrue();
        }
    }
}
