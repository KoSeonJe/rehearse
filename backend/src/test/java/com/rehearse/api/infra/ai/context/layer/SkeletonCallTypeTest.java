package com.rehearse.api.infra.ai.context.layer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkeletonCallType - callType 값 조회")
class SkeletonCallTypeTest {

    @Test
    @DisplayName("fromValue_resume_extractor_returns_present")
    void fromValue_resume_extractor_returns_present() {
        Optional<SkeletonCallType> result = SkeletonCallType.fromValue("resume_extractor");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(SkeletonCallType.RESUME_EXTRACTOR);
    }

    @Test
    @DisplayName("fromValue_resume_interview_planner_returns_present")
    void fromValue_resume_interview_planner_returns_present() {
        Optional<SkeletonCallType> result = SkeletonCallType.fromValue("resume_interview_planner");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(SkeletonCallType.RESUME_INTERVIEW_PLANNER);
    }

    @Test
    @DisplayName("fromValue_unknown_returns_empty")
    void fromValue_unknown_returns_empty() {
        Optional<SkeletonCallType> result = SkeletonCallType.fromValue("unknown_type");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fromValue_null_returns_empty")
    void fromValue_null_returns_empty() {
        Optional<SkeletonCallType> result = SkeletonCallType.fromValue(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("resume_interview_planner_skeleton_contains_forbidden_fields_warning")
    void resume_interview_planner_skeleton_contains_forbidden_fields_warning() {
        String skeleton = SkeletonCallType.RESUME_INTERVIEW_PLANNER.skeleton();

        assertThat(skeleton).contains("allocated_time_min");
        assertThat(skeleton).contains("max_turns");
        assertThat(skeleton).contains("estimated_duration_min");
    }

    @Test
    @DisplayName("resume_interview_planner_skeleton_contains_priority_only_principle")
    void resume_interview_planner_skeleton_contains_priority_only_principle() {
        String skeleton = SkeletonCallType.RESUME_INTERVIEW_PLANNER.skeleton();

        assertThat(skeleton).contains("priority");
        assertThat(skeleton).contains("JSON");
    }
}
