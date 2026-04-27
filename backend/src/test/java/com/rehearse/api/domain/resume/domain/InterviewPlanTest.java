package com.rehearse.api.domain.resume.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewPlanTest {

    private static ProjectPlan createProjectPlan(String projectId, int priority) {
        PlaygroundPhase playground = new PlaygroundPhase("소개해주세요.", List.of());
        ChainReference primary = new ChainReference(projectId + "::캐시", "캐시", 1, List.of(1, 2));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(primary), List.of());
        return new ProjectPlan(projectId, "프로젝트" + priority, priority, playground, interrogation);
    }

    @Test
    @DisplayName("정상 입력으로 InterviewPlan 이 생성된다")
    void interviewPlan_정상생성() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1), createProjectPlan("p2", 2));
        InterviewPlan interviewPlan = new InterviewPlan("plan_abc", 30, plans);

        assertThat(interviewPlan.sessionPlanId()).isEqualTo("plan_abc");
        assertThat(interviewPlan.durationHintMin()).isEqualTo(30);
        assertThat(interviewPlan.totalProjects()).isEqualTo(2);
        assertThat(interviewPlan.projectPlans()).hasSize(2);
    }

    @Test
    @DisplayName("projectPlans 는 불변 리스트로 반환된다")
    void interviewPlan_projectPlans_불변() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        InterviewPlan interviewPlan = new InterviewPlan("plan_abc", 30, plans);

        assertThatThrownBy(() -> interviewPlan.projectPlans().add(createProjectPlan("p2", 2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("sessionPlanId 가 null 이면 예외가 발생한다")
    void interviewPlan_sessionPlanId_null_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan(null, 30, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionPlanId 는 필수입니다");
    }

    @Test
    @DisplayName("sessionPlanId 가 blank 이면 예외가 발생한다")
    void interviewPlan_sessionPlanId_blank_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan("  ", 30, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionPlanId 는 필수입니다");
    }

    @Test
    @DisplayName("durationHintMin 이 0 이면 예외가 발생한다")
    void interviewPlan_durationHintMin_0_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", 0, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationHintMin 은 0 보다 커야 합니다");
    }

    @Test
    @DisplayName("durationHintMin 이 음수이면 예외가 발생한다")
    void interviewPlan_durationHintMin_음수_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", -5, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationHintMin 은 0 보다 커야 합니다");
    }

    @Test
    @DisplayName("projectPlans 가 null 이면 예외가 발생한다")
    void interviewPlan_projectPlans_null_reject() {
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", 30, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPlans 는 필수입니다");
    }

    @Test
    @DisplayName("totalProjects 와 projectPlans 크기가 다르면 예외가 발생한다")
    void interviewPlan_totalProjects_불일치_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", 30, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalProjects 와 projectPlans 크기가 일치하지 않습니다");
    }

    @Test
    @DisplayName("projectPlans priority 가 오름차순이 아니면 예외가 발생한다")
    void interviewPlan_priority_비오름차순_reject() {
        List<ProjectPlan> plans = List.of(
                createProjectPlan("p1", 2),
                createProjectPlan("p2", 1)
        );
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", 30, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority 는 중복 없이 오름차순이어야 합니다");
    }

    @Test
    @DisplayName("projectPlans priority 가 중복이면 예외가 발생한다")
    void interviewPlan_priority_중복_reject() {
        List<ProjectPlan> plans = List.of(
                createProjectPlan("p1", 1),
                createProjectPlan("p2", 1)
        );
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", 30, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority 는 중복 없이 오름차순이어야 합니다");
    }
}
