package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.resume.domain.ChainRef;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewPlanCache - RuntimeState 기반 인메모리 캐시")
class InterviewPlanCacheTest {

    @InjectMocks
    private InterviewPlanCache cache;

    @Mock
    private InterviewRuntimeStateStore runtimeStateStore;

    @Test
    @DisplayName("read_플랜반환_when_plan_exists_in_runtime_state")
    void read_returns_plan_when_plan_exists_in_runtime_state() {
        InterviewPlan plan = createFixturePlan();
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
        state.setInterviewPlan(plan);

        given(runtimeStateStore.get(1L)).willReturn(state);

        InterviewPlan result = cache.read(1L);

        assertThat(result).isEqualTo(plan);
    }

    @Test
    @DisplayName("read_null반환_when_plan_not_set_in_runtime_state")
    void read_returns_null_when_plan_not_set_in_runtime_state() {
        InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);

        given(runtimeStateStore.get(1L)).willReturn(state);

        InterviewPlan result = cache.read(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("read_null반환_when_session_not_initialized")
    void read_returns_null_when_session_not_initialized() {
        given(runtimeStateStore.get(1L)).willThrow(new IllegalStateException("not initialized"));

        InterviewPlan result = cache.read(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("write_RuntimeState_업데이트_when_session_initialized")
    void write_updates_runtime_state_with_plan() {
        InterviewPlan plan = createFixturePlan();
        willDoNothing().given(runtimeStateStore).update(eq(1L), any());

        cache.write(1L, plan);

        then(runtimeStateStore).should().update(eq(1L), any());
    }

    @Test
    @DisplayName("write_graceful_skip_when_session_not_initialized")
    void write_silently_skips_when_session_not_initialized() {
        InterviewPlan plan = createFixturePlan();
        willThrow(new IllegalStateException("not initialized"))
                .given(runtimeStateStore).update(eq(1L), any());

        cache.write(1L, plan);

        then(runtimeStateStore).should().update(eq(1L), any());
    }

    private InterviewPlan createFixturePlan() {
        ChainRef chain = new ChainRef("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요.", List.of("p1_c1"));
        ProjectPlan projectPlan = new ProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new InterviewPlan("plan_test", 30, 1, List.of(projectPlan));
    }
}
