package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.repository.InterviewPlanRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewPlanPersister - DB 영속")
class InterviewPlanPersisterTest {

    @InjectMocks
    private InterviewPlanPersister store;

    @Mock
    private InterviewPlanRepository planRepository;

    @Test
    @DisplayName("save_assigns_interviewId_and_persists")
    void save_assigns_interview_id_and_calls_repository() {
        InterviewPlan plan = createFixturePlan();

        store.save(1L, plan);

        assertThat(plan.getInterviewId()).isEqualTo(1L);
        then(planRepository).should().save(plan);
    }

    @Test
    @DisplayName("findByInterviewId_returns_plan_when_present")
    void findByInterviewId_returns_present_plan() {
        InterviewPlan plan = createFixturePlan();
        given(planRepository.findByInterviewId(1L)).willReturn(Optional.of(plan));

        Optional<InterviewPlan> result = store.findByInterviewId(1L);

        assertThat(result).contains(plan);
    }

    @Test
    @DisplayName("findByInterviewId_returns_empty_when_absent")
    void findByInterviewId_returns_empty_when_absent() {
        given(planRepository.findByInterviewId(99L)).willReturn(Optional.empty());

        Optional<InterviewPlan> result = store.findByInterviewId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save_falls_back_to_refetch_when_unique_violation")
    void save_falls_back_to_refetch_on_data_integrity_violation() {
        InterviewPlan plan = createFixturePlan();
        InterviewPlan existing = createFixturePlan();
        existing.assignToInterview(1L);

        willThrow(DataIntegrityViolationException.class).given(planRepository).save(any());
        given(planRepository.findByInterviewId(1L)).willReturn(Optional.of(existing));

        assertThatCode(() -> store.save(1L, plan)).doesNotThrowAnyException();
        then(planRepository).should().findByInterviewId(1L);
    }

    private InterviewPlan createFixturePlan() {
        ChainReference chain = new ChainReference("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요.", List.of("p1_c1"));
        ProjectPlan projectPlan = new ProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new InterviewPlan("plan_test", 30, List.of(projectPlan));
    }
}
