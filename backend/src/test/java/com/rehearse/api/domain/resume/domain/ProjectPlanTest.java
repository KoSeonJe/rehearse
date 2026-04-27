package com.rehearse.api.domain.resume.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectPlanTest {

    private static PlaygroundPhase playgroundPhase() {
        return new PlaygroundPhase("프로젝트 소개해주세요.", List.of("p1_c1"));
    }

    private static InterrogationPhase interrogationPhase() {
        ChainReference primary = new ChainReference("p1::캐시", "캐시", 1, List.of(1, 2));
        return new InterrogationPhase(List.of(primary), List.of());
    }

    @Test
    @DisplayName("정상 입력으로 ProjectPlan 이 생성된다")
    void projectPlan_정상생성() {
        ProjectPlan plan = new ProjectPlan("p1", "주문서비스", 1, playgroundPhase(), interrogationPhase());

        assertThat(plan.projectId()).isEqualTo("p1");
        assertThat(plan.projectName()).isEqualTo("주문서비스");
        assertThat(plan.priority()).isEqualTo(1);
    }

    @Test
    @DisplayName("projectId 가 null 이면 예외가 발생한다")
    void projectPlan_projectId_null_reject() {
        assertThatThrownBy(() -> new ProjectPlan(null, "주문서비스", 1, playgroundPhase(), interrogationPhase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId 는 필수입니다");
    }

    @Test
    @DisplayName("projectId 가 blank 이면 예외가 발생한다")
    void projectPlan_projectId_blank_reject() {
        assertThatThrownBy(() -> new ProjectPlan("  ", "주문서비스", 1, playgroundPhase(), interrogationPhase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId 는 필수입니다");
    }

    @Test
    @DisplayName("projectName 이 null 이면 예외가 발생한다")
    void projectPlan_projectName_null_reject() {
        assertThatThrownBy(() -> new ProjectPlan("p1", null, 1, playgroundPhase(), interrogationPhase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectName 은 필수입니다");
    }

    @Test
    @DisplayName("priority 가 0 이면 예외가 발생한다")
    void projectPlan_priority_0_reject() {
        assertThatThrownBy(() -> new ProjectPlan("p1", "주문서비스", 0, playgroundPhase(), interrogationPhase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority 는 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("priority 가 음수이면 예외가 발생한다")
    void projectPlan_priority_음수_reject() {
        assertThatThrownBy(() -> new ProjectPlan("p1", "주문서비스", -1, playgroundPhase(), interrogationPhase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority 는 1 이상이어야 합니다");
    }

    @Test
    @DisplayName("playgroundPhase 가 null 이면 예외가 발생한다")
    void projectPlan_playgroundPhase_null_reject() {
        assertThatThrownBy(() -> new ProjectPlan("p1", "주문서비스", 1, null, interrogationPhase()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("playgroundPhase 는 필수입니다");
    }

    @Test
    @DisplayName("interrogationPhase 가 null 이면 예외가 발생한다")
    void projectPlan_interrogationPhase_null_reject() {
        assertThatThrownBy(() -> new ProjectPlan("p1", "주문서비스", 1, playgroundPhase(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interrogationPhase 는 필수입니다");
    }
}
