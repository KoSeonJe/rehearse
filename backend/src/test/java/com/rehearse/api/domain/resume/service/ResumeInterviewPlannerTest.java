package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.CandidateLevel;
import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.ChainStep;
import com.rehearse.api.domain.resume.domain.ClaimType;
import com.rehearse.api.domain.resume.domain.InterrogationChain;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.Priority;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.domain.resume.domain.ResumeClaim;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.StepType;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.ResumeInterviewPlanAdapter;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.prompt.ResumeInterviewPlannerPromptBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeInterviewPlanner - 오케스트레이션 (PromptBuilder + Adapter + Validator)")
class ResumeInterviewPlannerTest {

    @InjectMocks
    private ResumeInterviewPlanner planner;

    @Mock
    private ResumeInterviewPlannerPromptBuilder promptBuilder;

    @Mock
    private ResumeInterviewPlanAdapter planAdapter;

    @Mock
    private ResumeInterviewPlanValidator planValidator;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private ResumeSkeleton skeleton;
    private ChatRequest stubRequest;
    private InterviewPlan stubPlan;

    @BeforeEach
    void setUp() {
        skeleton = createFixtureSkeleton();
        stubRequest = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "stub")))
                .callType("resume_interview_planner")
                .build();
        stubPlan = createFixturePlan(30);
    }

    @Test
    @DisplayName("plan_정상생성_when_skeleton_is_mapped_and_validated")
    void plan_returns_plan_when_adapter_and_validator_succeed() {
        given(promptBuilder.build(any(), eq(30), eq("JUNIOR"), eq("resume_interview_planner")))
                .willReturn(stubRequest);
        given(planAdapter.execute(stubRequest, 30)).willReturn(stubPlan);

        InterviewPlan result = planner.plan(skeleton, 30);

        assertThat(result).isSameAs(stubPlan);
        then(planValidator).should().validate(skeleton, stubPlan);
    }

    @Test
    @DisplayName("plan_default_user_level_MID_when_skeleton_level_is_null")
    void plan_uses_default_user_level_when_skeleton_level_null() {
        ResumeSkeleton skeletonNoLevel = new ResumeSkeleton(
                "r_test", "hash", null, "backend", skeleton.projects(), Map.of());
        given(promptBuilder.build(any(), eq(30), eq("MID"), any())).willReturn(stubRequest);
        given(planAdapter.execute(stubRequest, 30)).willReturn(stubPlan);

        planner.plan(skeletonNoLevel, 30);

        then(promptBuilder).should().build(any(), eq(30), eq("MID"), any());
    }

    @Test
    @DisplayName("plan_validator_예외전파_when_validator_throws")
    void plan_propagates_validator_exception() {
        given(promptBuilder.build(any(), anyInt(), any(), any())).willReturn(stubRequest);
        given(planAdapter.execute(stubRequest, 30)).willReturn(stubPlan);
        willThrow(new BusinessException(ResumePlannerErrorCode.ORPHAN_CHAIN))
                .given(planValidator).validate(skeleton, stubPlan);

        assertThatThrownBy(() -> planner.plan(skeleton, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.ORPHAN_CHAIN.getCode()));
    }

    @Test
    @DisplayName("plan_INVALID_PLAN_when_skeleton_serialization_fails")
    void plan_throws_invalid_plan_when_serialization_fails() throws JsonProcessingException {
        given(objectMapper.writeValueAsString(skeleton))
                .willThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> planner.plan(skeleton, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    private ResumeSkeleton createFixtureSkeleton() {
        List<ChainStep> steps = List.of(
                new ChainStep(1, StepType.WHAT, "WHAT"),
                new ChainStep(2, StepType.HOW, "HOW"),
                new ChainStep(3, StepType.WHY_MECH, "WHY"),
                new ChainStep(4, StepType.TRADEOFF, "T")
        );
        List<ResumeClaim> claims = List.of(
                new ResumeClaim("p1_c1", "claim", ClaimType.IMPLEMENTATION, Priority.HIGH, List.of())
        );
        Project project = new Project("p1", claims, List.of(new InterrogationChain("Redis", 0.9, steps)));
        return new ResumeSkeleton("r1", "h1", CandidateLevel.JUNIOR, "backend", List.of(project), Map.of());
    }

    private InterviewPlan createFixturePlan(int durationMin) {
        ChainReference chain = new ChainReference("p1::Redis", "Redis", 1, List.of(1, 2));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("opener", List.of("p1_c1"));
        ProjectPlan project = new ProjectPlan("p1", "Project A", 1, playground, interrogation);
        return new InterviewPlan("plan_test", durationMin, List.of(project));
    }
}
