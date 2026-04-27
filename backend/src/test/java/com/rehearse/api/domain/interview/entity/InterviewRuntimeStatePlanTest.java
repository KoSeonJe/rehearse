package com.rehearse.api.domain.interview.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearse.api.domain.resume.domain.ChainRef;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InterviewRuntimeState - InterviewPlan 필드 관리")
class InterviewRuntimeStatePlanTest {

    @Test
    @DisplayName("초기_interviewPlanCache_null")
    void initial_interview_plan_cache_is_null() {
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);

        assertThat(state.getInterviewPlanCache()).isNull();
    }

    @Test
    @DisplayName("setInterviewPlan_후_getInterviewPlanCache_일관성")
    void get_interview_plan_cache_returns_set_plan() {
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
        InterviewPlan plan = createFixturePlan();

        state.setInterviewPlan(plan);

        assertThat(state.getInterviewPlanCache()).isEqualTo(plan);
        assertThat(state.getInterviewPlanCache().sessionPlanId()).isEqualTo("plan_test");
        assertThat(state.getInterviewPlanCache().durationHintMin()).isEqualTo(30);
    }

    @Test
    @DisplayName("setInterviewPlan_덮어쓰기_when_called_twice")
    void set_interview_plan_overwrites_previous_value() {
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
        InterviewPlan first = createFixturePlan();
        InterviewPlan second = createFixturePlan("plan_second", 60);

        state.setInterviewPlan(first);
        state.setInterviewPlan(second);

        assertThat(state.getInterviewPlanCache().sessionPlanId()).isEqualTo("plan_second");
        assertThat(state.getInterviewPlanCache().durationHintMin()).isEqualTo(60);
    }

    @Test
    @DisplayName("resumeSkeletonCache와_interviewPlanCache_독립적으로_관리됨")
    void resume_skeleton_cache_and_interview_plan_cache_are_independent() {
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
        InterviewPlan plan = createFixturePlan();

        state.setInterviewPlan(plan);

        assertThat(state.getResumeSkeletonCache()).isNull();
        assertThat(state.getInterviewPlanCache()).isNotNull();
    }

    private InterviewPlan createFixturePlan() {
        return createFixturePlan("plan_test", 30);
    }

    private InterviewPlan createFixturePlan(String sessionPlanId, int durationHintMin) {
        ChainRef chain = new ChainRef("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요.", List.of("p1_c1"));
        ProjectPlan projectPlan = new ProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new InterviewPlan(sessionPlanId, durationHintMin, 1, List.of(projectPlan));
    }
}
