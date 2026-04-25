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
    private static final String CONNECTOR = OffTopicMarker.CONNECTOR;
    private static final String MAIN_Q = "HashMap 충돌을 설명해주세요.";

    private FollowUpExchange offTopicByMeta() {
        return new FollowUpExchange("정상 텍스트", "잡담", OffTopicMarker.FOLLOW_UP_TYPE);
    }

    private FollowUpExchange offTopicByLegacyText() {
        return new FollowUpExchange("리드인 " + CONNECTOR + " " + MAIN_Q, "잡담");
    }

    private FollowUpExchange normalExchange() {
        return new FollowUpExchange("정상 꼬리 질문", "기술 답변");
    }

    @Nested
    @DisplayName("countRecentConsecutive - 메타 필드(followUpType) 우선")
    class MetaFieldPriority {

        @Test
        @DisplayName("followUpType=OFF_TOPIC_REDIRECT 인 마지막 1개를 OFF_TOPIC 으로 카운트한다")
        void count_metaFieldOffTopic_returnsOne() {
            List<FollowUpExchange> exchanges = List.of(normalExchange(), offTopicByMeta());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(1);
        }

        @Test
        @DisplayName("followUpType 메타 필드로 연속 3회 모두 카운트된다")
        void count_threeMetaOffTopic_returnsThree() {
            List<FollowUpExchange> exchanges = List.of(offTopicByMeta(), offTopicByMeta(), offTopicByMeta());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("countRecentConsecutive - 텍스트 fallback (메타 필드 미상)")
    class LegacyTextFallback {

        @Test
        @DisplayName("followUpType이 null이지만 connector 텍스트가 포함되면 OFF_TOPIC 으로 인식한다")
        void count_legacyText_recognizedAsOffTopic() {
            List<FollowUpExchange> exchanges = List.of(normalExchange(), offTopicByLegacyText());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countRecentConsecutive - 일반 케이스")
    class GeneralCases {

        @Test
        @DisplayName("previousExchanges가 null이면 0")
        void count_nullExchanges_returnsZero() {
            assertThat(detector.countRecentConsecutive(null)).isEqualTo(0);
        }

        @Test
        @DisplayName("previousExchanges가 빈 리스트이면 0")
        void count_emptyExchanges_returnsZero() {
            assertThat(detector.countRecentConsecutive(List.of())).isEqualTo(0);
        }

        @Test
        @DisplayName("OFF_TOPIC 표시가 없는 교환만 있으면 0")
        void count_noOffTopicMarker_returnsZero() {
            List<FollowUpExchange> exchanges = List.of(normalExchange(), normalExchange());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(0);
        }

        @Test
        @DisplayName("중간에 정상 교환이 끼어들면 연속 카운트가 리셋된다")
        void count_normalExchangeInterrupts_resetsCount() {
            List<FollowUpExchange> exchanges = List.of(
                    offTopicByMeta(), offTopicByMeta(), normalExchange(), offTopicByMeta());
            assertThat(detector.countRecentConsecutive(exchanges)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("shouldEscalate - 임계값 도달 여부")
    class ShouldEscalate {

        @Test
        @DisplayName("consecutive + 1이 limit 미만이면 false")
        void shouldEscalate_belowLimit_returnsFalse() {
            assertThat(detector.shouldEscalate(1, 3)).isFalse();
        }

        @Test
        @DisplayName("consecutive + 1이 limit과 같으면 true")
        void shouldEscalate_exactlyAtLimit_returnsTrue() {
            assertThat(detector.shouldEscalate(2, 3)).isTrue();
        }

        @Test
        @DisplayName("consecutive + 1이 limit을 초과해도 true")
        void shouldEscalate_aboveLimit_returnsTrue() {
            assertThat(detector.shouldEscalate(3, 3)).isTrue();
        }
    }
}
