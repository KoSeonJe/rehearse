package com.rehearse.api.domain.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.domain.CandidateLevel;
import com.rehearse.api.domain.resume.domain.ChainStep;
import com.rehearse.api.domain.resume.domain.ClaimType;
import com.rehearse.api.domain.resume.domain.InterrogationChain;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.Priority;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.domain.resume.domain.ResumeClaim;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.StepType;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedChainRef;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedInterrogationPhase;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedPlaygroundPhase;
import com.rehearse.api.infra.ai.dto.GeneratedInterviewPlan.GeneratedProjectPlan;
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
@DisplayName("ResumeInterviewPlanner - LLM 호출 + 검증 + 도메인 매핑")
class ResumeInterviewPlannerTest {

    @InjectMocks
    private ResumeInterviewPlanner planner;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    @Mock
    private ResumeInterviewPlannerPromptBuilder promptBuilder;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private ResumeSkeleton skeleton;
    private ChatRequest mockRequest;
    private ChatResponse mockResponse;

    @BeforeEach
    void setUp() {
        skeleton = createFixtureSkeleton();
        mockRequest = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "stub")))
                .temperature(0.3)
                .maxTokens(2048)
                .callType("resume_interview_planner")
                .build();
        mockResponse = new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);

        given(promptBuilder.build(any(), eq(30), any(), any())).willReturn(mockRequest);
        given(aiClient.chat(mockRequest)).willReturn(mockResponse);
    }

    @Test
    @DisplayName("plan_정상생성_when_valid_skeleton_and_duration")
    void plan_returns_valid_interview_plan_when_skeleton_and_duration_are_valid() {
        GeneratedInterviewPlan raw = createValidGeneratedPlan(30);
        given(aiResponseParser.parseOrRetry(eq(mockResponse), eq(GeneratedInterviewPlan.class), eq(aiClient), eq(mockRequest)))
                .willReturn(raw);

        InterviewPlan result = planner.plan(skeleton, 30);

        assertThat(result.sessionPlanId()).isEqualTo("plan_test");
        assertThat(result.durationHintMin()).isEqualTo(30);
        assertThat(result.totalProjects()).isEqualTo(2);
        assertThat(result.projectPlans()).hasSize(2);
        assertThat(result.projectPlans().get(0).priority()).isEqualTo(1);
        assertThat(result.projectPlans().get(1).priority()).isEqualTo(2);
    }

    @Test
    @DisplayName("plan_orphan_chain_검출_when_chain_id_not_in_skeleton")
    void plan_throws_orphan_chain_when_chain_id_not_in_skeleton() {
        GeneratedInterviewPlan raw = createPlanWithOrphanChain();
        given(aiResponseParser.parseOrRetry(any(), eq(GeneratedInterviewPlan.class), any(), any()))
                .willReturn(raw);

        assertThatThrownBy(() -> planner.plan(skeleton, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.ORPHAN_CHAIN.getCode()));
    }

    @Test
    @DisplayName("plan_orphan_claim_검출_when_claim_id_not_in_skeleton")
    void plan_throws_orphan_claim_when_claim_id_not_in_skeleton() {
        GeneratedInterviewPlan raw = createPlanWithOrphanClaim();
        given(aiResponseParser.parseOrRetry(any(), eq(GeneratedInterviewPlan.class), any(), any()))
                .willReturn(raw);

        assertThatThrownBy(() -> planner.plan(skeleton, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.ORPHAN_CLAIM.getCode()));
    }

    @Test
    @DisplayName("plan_invalid_plan_when_chain_id_missing_double_colon")
    void plan_throws_invalid_plan_when_chain_id_missing_double_colon() {
        GeneratedInterviewPlan raw = createPlanWithInvalidChainIdFormat();
        given(aiResponseParser.parseOrRetry(any(), eq(GeneratedInterviewPlan.class), any(), any()))
                .willReturn(raw);

        assertThatThrownBy(() -> planner.plan(skeleton, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    @Test
    @DisplayName("plan_duration_hint_강제덮어쓰기_when_llm_returns_different_value")
    void plan_overrides_duration_hint_when_llm_returns_different_value() {
        GeneratedInterviewPlan raw = createValidGeneratedPlan(99);
        given(aiResponseParser.parseOrRetry(any(), eq(GeneratedInterviewPlan.class), any(), any()))
                .willReturn(raw);

        InterviewPlan result = planner.plan(skeleton, 30);

        assertThat(result.durationHintMin()).isEqualTo(30);
    }

    @Test
    @DisplayName("plan_parseOrRetry_한번만_호출됨")
    void plan_calls_parse_or_retry_exactly_once() {
        GeneratedInterviewPlan raw = createValidGeneratedPlan(30);
        given(aiResponseParser.parseOrRetry(any(), eq(GeneratedInterviewPlan.class), any(), any()))
                .willReturn(raw);

        planner.plan(skeleton, 30);

        then(aiResponseParser).should().parseOrRetry(
                eq(mockResponse), eq(GeneratedInterviewPlan.class), eq(aiClient), eq(mockRequest));
    }

    // --- fixtures ---

    private ResumeSkeleton createFixtureSkeleton() {
        List<ChainStep> steps = List.of(
                new ChainStep(1, StepType.WHAT, "WHAT question"),
                new ChainStep(2, StepType.HOW, "HOW question"),
                new ChainStep(3, StepType.WHY_MECH, "WHY_MECH question"),
                new ChainStep(4, StepType.TRADEOFF, "TRADEOFF question")
        );

        List<ResumeClaim> p1Claims = List.of(
                new ResumeClaim("p1_c1", "claim text 1", ClaimType.IMPLEMENTATION, Priority.HIGH, List.of()),
                new ResumeClaim("p1_c2", "claim text 2", ClaimType.IMPLEMENTATION, Priority.MEDIUM, List.of()),
                new ResumeClaim("p1_c3", "claim text 3", ClaimType.IMPACT_METRIC, Priority.LOW, List.of())
        );
        List<InterrogationChain> p1Chains = List.of(
                new InterrogationChain("Redis", 0.9, steps),
                new InterrogationChain("JPA", 0.8, steps)
        );

        List<ResumeClaim> p2Claims = List.of(
                new ResumeClaim("p2_c1", "claim text 4", ClaimType.IMPLEMENTATION, Priority.HIGH, List.of())
        );
        List<InterrogationChain> p2Chains = List.of(
                new InterrogationChain("Kafka", 0.85, steps)
        );

        Project project1 = new Project("p1", p1Claims, p1Chains);
        Project project2 = new Project("p2", p2Claims, p2Chains);

        return new ResumeSkeleton("r_test", "hash_abc", CandidateLevel.JUNIOR, "backend",
                List.of(project1, project2), Map.of());
    }

    private GeneratedInterviewPlan createValidGeneratedPlan(int durationHintMin) {
        GeneratedChainRef primaryChain = new GeneratedChainRef("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        GeneratedChainRef backupChain = new GeneratedChainRef("p1::JPA", "JPA", 2, List.of(1, 2));
        GeneratedInterrogationPhase interrogation1 = new GeneratedInterrogationPhase(
                List.of(primaryChain), List.of(backupChain));
        GeneratedPlaygroundPhase playground1 = new GeneratedPlaygroundPhase(
                "프로젝트를 소개해주세요.", List.of("p1_c1", "p1_c2"));
        GeneratedProjectPlan plan1 = new GeneratedProjectPlan("p1", "Project Alpha", 1, playground1, interrogation1);

        GeneratedChainRef primaryChain2 = new GeneratedChainRef("p2::Kafka", "Kafka", 1, List.of(1, 2, 3, 4));
        GeneratedInterrogationPhase interrogation2 = new GeneratedInterrogationPhase(
                List.of(primaryChain2), List.of());
        GeneratedPlaygroundPhase playground2 = new GeneratedPlaygroundPhase(
                "두 번째 프로젝트를 소개해주세요.", List.of("p2_c1"));
        GeneratedProjectPlan plan2 = new GeneratedProjectPlan("p2", "Project Beta", 2, playground2, interrogation2);

        return new GeneratedInterviewPlan("plan_test", durationHintMin, 2, List.of(plan1, plan2));
    }

    private GeneratedInterviewPlan createPlanWithOrphanChain() {
        GeneratedChainRef orphan = new GeneratedChainRef("p1::NonExistentTopic", "NonExistentTopic", 1, List.of(1, 2));
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(orphan), List.of());
        GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("질문", List.of("p1_c1"));
        GeneratedProjectPlan plan = new GeneratedProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new GeneratedInterviewPlan("plan_orphan_chain", 30, 1, List.of(plan));
    }

    private GeneratedInterviewPlan createPlanWithOrphanClaim() {
        GeneratedChainRef chain = new GeneratedChainRef("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(chain), List.of());
        GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("질문", List.of("p1_c1", "p1_c_nonexistent"));
        GeneratedProjectPlan plan = new GeneratedProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new GeneratedInterviewPlan("plan_orphan_claim", 30, 1, List.of(plan));
    }

    private GeneratedInterviewPlan createPlanWithInvalidChainIdFormat() {
        GeneratedChainRef invalidChain = new GeneratedChainRef("no-double-colon", "Redis", 1, List.of(1, 2));
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(invalidChain), List.of());
        GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("질문", List.of("p1_c1"));
        GeneratedProjectPlan plan = new GeneratedProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new GeneratedInterviewPlan("plan_invalid_format", 30, 1, List.of(plan));
    }
}
