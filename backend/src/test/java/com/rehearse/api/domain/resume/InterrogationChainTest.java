package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.domain.InterrogationChain;
import com.rehearse.api.domain.resume.domain.ChainStep;
import com.rehearse.api.domain.resume.domain.StepType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InterrogationChain - 4단 심문 체인 도메인 규칙")
class InterrogationChainTest {

    @Test
    @DisplayName("valid_chain_created_when_all_four_step_types_present_exactly_once")
    void valid_chain_created_when_all_four_step_types_present_exactly_once() {
        InterrogationChain chain = new InterrogationChain("rate-limiting", 0.9, List.of(
                new ChainStep(1, StepType.WHAT, "Rate limiting이란 무엇인가요?"),
                new ChainStep(2, StepType.HOW, "어떻게 구현하셨나요?"),
                new ChainStep(3, StepType.WHY_MECH, "왜 그 방식을 선택하셨나요?"),
                new ChainStep(4, StepType.TRADEOFF, "어떤 트레이드오프가 있었나요?")
        ));

        assertThat(chain.topic()).isEqualTo("rate-limiting");
        assertThat(chain.steps()).hasSize(4);
    }

    @Test
    @DisplayName("steps_are_immutable_after_construction")
    void steps_are_immutable_after_construction() {
        InterrogationChain chain = new InterrogationChain("caching", 0.9, List.of(
                new ChainStep(1, StepType.WHAT, "Q1"),
                new ChainStep(2, StepType.HOW, "Q2"),
                new ChainStep(3, StepType.WHY_MECH, "Q3"),
                new ChainStep(4, StepType.TRADEOFF, "Q4")
        ));

        assertThatThrownBy(() -> chain.steps().add(new ChainStep(5, StepType.WHAT, "extra")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("throws_when_steps_is_null")
    void throws_when_steps_is_null() {
        assertThatThrownBy(() -> new InterrogationChain("topic", 0.5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("throws_when_steps_is_empty")
    void throws_when_steps_is_empty() {
        assertThatThrownBy(() -> new InterrogationChain("topic", 0.5, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("throws_when_step_type_is_missing")
    void throws_when_step_type_is_missing() {
        assertThatThrownBy(() -> new InterrogationChain("caching", 0.8, List.of(
                new ChainStep(1, StepType.WHAT, "Q1"),
                new ChainStep(2, StepType.HOW, "Q2"),
                new ChainStep(3, StepType.WHY_MECH, "Q3")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WHAT, HOW, WHY_MECH, TRADEOFF");
    }

    @Test
    @DisplayName("throws_when_step_type_is_duplicated")
    void throws_when_step_type_is_duplicated() {
        assertThatThrownBy(() -> new InterrogationChain("caching", 0.8, List.of(
                new ChainStep(1, StepType.WHAT, "Q1"),
                new ChainStep(2, StepType.WHAT, "duplicate"),
                new ChainStep(3, StepType.HOW, "Q3"),
                new ChainStep(4, StepType.WHY_MECH, "Q4")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate StepType");
    }

    @Test
    @DisplayName("throws_when_size_is_not_four_and_has_extra_valid_types")
    void throws_when_size_is_not_four_and_has_extra_valid_types() {
        // TRADEOFF 누락 + 다른 타입 추가 불가능 → size가 달라도 4종 세트 아니면 예외
        assertThatThrownBy(() -> new InterrogationChain("topic", 0.9, List.of(
                new ChainStep(1, StepType.WHAT, "Q1"),
                new ChainStep(2, StepType.HOW, "Q2"),
                new ChainStep(3, StepType.WHY_MECH, "Q3"),
                new ChainStep(4, StepType.WHY_MECH, "duplicate")
        )))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
