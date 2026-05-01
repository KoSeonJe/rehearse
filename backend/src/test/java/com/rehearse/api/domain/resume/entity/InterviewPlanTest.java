package com.rehearse.api.domain.resume.entity;

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
        InterviewPlan interviewPlan = new InterviewPlan("plan_abc", plans);

        assertThat(interviewPlan.sessionPlanId()).isEqualTo("plan_abc");
        assertThat(interviewPlan.totalProjects()).isEqualTo(2);
        assertThat(interviewPlan.projectPlans()).hasSize(2);
    }

    @Test
    @DisplayName("projectPlans 는 불변 리스트로 반환된다")
    void interviewPlan_projectPlans_불변() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        InterviewPlan interviewPlan = new InterviewPlan("plan_abc", plans);

        assertThatThrownBy(() -> interviewPlan.projectPlans().add(createProjectPlan("p2", 2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("sessionPlanId 가 null 이면 예외가 발생한다")
    void interviewPlan_sessionPlanId_null_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan(null, plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionPlanId 는 필수입니다");
    }

    @Test
    @DisplayName("sessionPlanId 가 blank 이면 예외가 발생한다")
    void interviewPlan_sessionPlanId_blank_reject() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1));
        assertThatThrownBy(() -> new InterviewPlan("  ", plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionPlanId 는 필수입니다");
    }

    @Test
    @DisplayName("projectPlans 가 null 이면 예외가 발생한다")
    void interviewPlan_projectPlans_null_reject() {
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPlans 는 필수입니다");
    }

    @Test
    @DisplayName("totalProjects 는 projectPlans 크기에서 자동 도출된다")
    void interviewPlan_totalProjects_derived_from_size() {
        List<ProjectPlan> plans = List.of(createProjectPlan("p1", 1), createProjectPlan("p2", 2));
        InterviewPlan plan = new InterviewPlan("plan_abc", plans);
        assertThat(plan.totalProjects()).isEqualTo(2);
    }

    @Test
    @DisplayName("projectPlans priority 가 오름차순이 아니면 예외가 발생한다")
    void interviewPlan_priority_비오름차순_reject() {
        List<ProjectPlan> plans = List.of(
                createProjectPlan("p1", 2),
                createProjectPlan("p2", 1)
        );
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", plans))
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
        assertThatThrownBy(() -> new InterviewPlan("plan_abc", plans))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority 는 중복 없이 오름차순이어야 합니다");
    }
}
