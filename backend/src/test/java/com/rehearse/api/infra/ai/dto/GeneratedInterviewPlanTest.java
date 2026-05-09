package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedInterviewPlan - LLM 응답 매핑 시점 검증")
class GeneratedInterviewPlanTest {

    private GeneratedInterviewPlan.GeneratedProjectPlan sampleProjectPlan() {
        return new GeneratedInterviewPlan.GeneratedProjectPlan(
                "p1", "name", 1,
                new GeneratedInterviewPlan.GeneratedPlaygroundPhase("opener", null),
                new GeneratedInterviewPlan.GeneratedInterrogationPhase(null, null));
    }

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("sessionPlanId 와 projectPlans 가 있으면 인스턴스를 생성한다")
        void should_construct_when_required_fields_present() {
            GeneratedInterviewPlan plan = new GeneratedInterviewPlan(
                    "session-1", 1, List.of(sampleProjectPlan()));
            assertThat(plan.sessionPlanId()).isEqualTo("session-1");
            assertThat(plan.projectPlans()).hasSize(1);
        }

        @Test
        @DisplayName("nested record 의 null 리스트는 빈 리스트로 정규화된다")
        void should_normalize_nested_null_lists_to_empty() {
            GeneratedInterviewPlan.GeneratedInterrogationPhase phase =
                    new GeneratedInterviewPlan.GeneratedInterrogationPhase(null, null);
            assertThat(phase.primaryChains()).isEmpty();
            assertThat(phase.backupChains()).isEmpty();
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("sessionPlanId 가 공백이면 거절한다")
        void should_reject_when_session_plan_id_blank() {
            assertThatThrownBy(() -> new GeneratedInterviewPlan(
                    "  ", 1, List.of(sampleProjectPlan())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("projectPlans 가 비어있으면 거절한다")
        void should_reject_when_project_plans_empty() {
            assertThatThrownBy(() -> new GeneratedInterviewPlan(
                    "session-1", 0, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
