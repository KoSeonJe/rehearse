package com.rehearse.api.infra.ai.dto;

import com.rehearse.api.domain.feedback.session.vo.DeliverySection;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedSessionFeedback - LLM 응답 매핑 시점 검증")
class GeneratedSessionFeedbackTest {

    private OverallSection overall() {
        return new OverallSection(Map.of("clarity", 0.7), "L2", "narr", "cov");
    }

    private DeliverySection delivery() {
        return new DeliverySection("음", "안정적", "느리게 말하기");
    }

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("nested 5종이 모두 있으면 인스턴스를 생성한다")
        void should_construct_when_all_sections_present() {
            GeneratedSessionFeedback g = new GeneratedSessionFeedback(
                    overall(), List.of(), List.of(), delivery(), List.of());
            assertThat(g.overall()).isNotNull();
            assertThat(g.delivery()).isNotNull();
        }

        @Test
        @DisplayName("nested 리스트 null 은 빈 리스트로 정규화된다")
        void should_normalize_null_lists_to_empty() {
            GeneratedSessionFeedback g = new GeneratedSessionFeedback(
                    overall(), null, null, delivery(), null);
            assertThat(g.strengths()).isEmpty();
            assertThat(g.gaps()).isEmpty();
            assertThat(g.weekPlan()).isEmpty();
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("overall 이 null 이면 거절한다")
        void should_reject_when_overall_null() {
            assertThatThrownBy(() -> new GeneratedSessionFeedback(
                    null, List.of(), List.of(), delivery(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("delivery null 은 허용된다 (delivery 입력 없는 케이스 — parser 가 cardinality 검증)")
        void should_allow_when_delivery_null() {
            GeneratedSessionFeedback g = new GeneratedSessionFeedback(
                    overall(), List.of(), List.of(), null, List.of());
            assertThat(g.delivery()).isNull();
        }
    }
}
