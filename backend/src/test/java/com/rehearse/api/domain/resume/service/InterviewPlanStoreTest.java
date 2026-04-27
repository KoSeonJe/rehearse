package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.ChainRef;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.domain.resume.entity.InterviewPlanEntity;
import com.rehearse.api.domain.resume.repository.InterviewPlanRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewPlanStore - DB 저장/조회 + 직렬화/역직렬화")
class InterviewPlanStoreTest {

    @InjectMocks
    private InterviewPlanStore store;

    @Mock
    private InterviewPlanRepository planRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("save_라운드트립_when_plan_serialized_and_saved")
    void save_persists_entity_when_plan_is_valid() throws Exception {
        InterviewPlan plan = createFixturePlan();

        store.save(1L, plan);

        then(planRepository).should().save(any(InterviewPlanEntity.class));
    }

    @Test
    @DisplayName("findByInterviewId_역직렬화_성공_when_entity_exists")
    void findByInterviewId_returns_deserialized_plan_when_entity_exists() throws Exception {
        InterviewPlan original = createFixturePlan();
        String json = new ObjectMapper().writeValueAsString(original);
        InterviewPlanEntity entity = InterviewPlanEntity.builder()
                .interviewId(1L)
                .planJson(json)
                .build();

        given(planRepository.findByInterviewId(1L)).willReturn(Optional.of(entity));

        Optional<InterviewPlan> result = store.findByInterviewId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().sessionPlanId()).isEqualTo("plan_test");
        assertThat(result.get().durationHintMin()).isEqualTo(30);
    }

    @Test
    @DisplayName("findByInterviewId_빈값_반환_when_entity_not_found")
    void findByInterviewId_returns_empty_when_no_entity() {
        given(planRepository.findByInterviewId(99L)).willReturn(Optional.empty());

        Optional<InterviewPlan> result = store.findByInterviewId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save_UNIQUE위반시_재조회_폴백_when_data_integrity_violation")
    void save_falls_back_to_refetch_when_unique_constraint_violated() throws Exception {
        InterviewPlan plan = createFixturePlan();
        InterviewPlanEntity existing = InterviewPlanEntity.builder()
                .interviewId(1L)
                .planJson("{}")
                .build();

        willThrow(DataIntegrityViolationException.class).given(planRepository).save(any());
        given(planRepository.findByInterviewId(1L)).willReturn(Optional.of(existing));

        store.save(1L, plan);

        then(planRepository).should().findByInterviewId(1L);
    }

    @Test
    @DisplayName("save_직렬화_실패시_BusinessException_발생")
    void save_throws_business_exception_when_serialization_fails() throws Exception {
        InterviewPlan plan = createFixturePlan();
        willThrow(new JsonProcessingException("serialize error") {})
                .given(objectMapper).writeValueAsString(plan);

        assertThatThrownBy(() -> store.save(1L, plan))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    @Test
    @DisplayName("findByInterviewId_역직렬화_실패시_BusinessException_발생")
    void findByInterviewId_throws_business_exception_when_json_is_malformed() throws Exception {
        InterviewPlanEntity entity = InterviewPlanEntity.builder()
                .interviewId(1L)
                .planJson("invalid-json")
                .build();

        given(planRepository.findByInterviewId(1L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> store.findByInterviewId(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
    }

    private InterviewPlan createFixturePlan() {
        ChainRef chain = new ChainRef("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요.", List.of("p1_c1"));
        ProjectPlan projectPlan = new ProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new InterviewPlan("plan_test", 30, 1, List.of(projectPlan));
    }
}
