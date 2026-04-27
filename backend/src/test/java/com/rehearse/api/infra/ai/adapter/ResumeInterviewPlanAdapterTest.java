package com.rehearse.api.infra.ai.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.rehearse.api.domain.resume.domain.InterviewPlan;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeInterviewPlanAdapter - LLM 호출 + DTO 매핑")
class ResumeInterviewPlanAdapterTest {

    @InjectMocks
    private ResumeInterviewPlanAdapter adapter;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    private ChatRequest request;
    private ChatResponse response;

    @BeforeEach
    void setUp() {
        request = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "stub")))
                .callType("resume_interview_planner")
                .build();
        response = new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
        given(aiClient.chat(request)).willReturn(response);
    }

    @Test
    @DisplayName("execute_정상매핑_priority_오름차순_when_unsorted_input")
    void execute_sorts_project_plans_by_priority_ascending() {
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                "plan_test", 30, 2,
                List.of(
                        project("p2", 2, "p2::Kafka"),
                        project("p1", 1, "p1::Redis")
                )
        );
        given(aiResponseParser.parseOrRetry(eq(response), eq(GeneratedInterviewPlan.class), eq(aiClient), eq(request)))
                .willReturn(raw);

        InterviewPlan result = adapter.execute(request, 30);

        assertThat(result.projectPlans().get(0).priority()).isEqualTo(1);
        assertThat(result.projectPlans().get(1).priority()).isEqualTo(2);
        assertThat(result.totalProjects()).isEqualTo(2);
    }

    @Test
    @DisplayName("execute_duration_hint_강제덮어쓰기_when_llm_returns_different")
    void execute_overrides_duration_hint_when_llm_returns_different_value() {
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                "plan_test", 99, 1, List.of(project("p1", 1, "p1::Redis")));
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        InterviewPlan result = adapter.execute(request, 30);

        assertThat(result.durationHintMin()).isEqualTo(30);
    }

    @Test
    @DisplayName("execute_INVALID_PLAN_when_chain_id_missing_separator")
    void execute_throws_invalid_plan_when_chain_id_missing_separator() {
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                "plan_test", 30, 1, List.of(project("p1", 1, "no-separator")));
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        assertThatThrownBy(() -> adapter.execute(request, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    @Test
    @DisplayName("execute_INVALID_PLAN_when_playground_phase_null")
    void execute_throws_invalid_plan_when_playground_phase_null() {
        GeneratedChainRef chain = new GeneratedChainRef("p1::Redis", "Redis", 1, List.of(1, 2));
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(chain), List.of());
        GeneratedProjectPlan plan = new GeneratedProjectPlan("p1", "Alpha", 1, null, interrogation);
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan("plan_test", 30, 1, List.of(plan));
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        assertThatThrownBy(() -> adapter.execute(request, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    @Test
    @DisplayName("execute_INVALID_PLAN_when_project_plans_null")
    void execute_throws_invalid_plan_when_project_plans_null() {
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan("plan_test", 30, 0, null);
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        assertThatThrownBy(() -> adapter.execute(request, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    private GeneratedProjectPlan project(String projectId, int priority, String chainId) {
        GeneratedChainRef chain = new GeneratedChainRef(chainId, chainId.contains("::") ? chainId.split("::")[1] : "topic", 1, List.of(1, 2));
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(chain), List.of());
        GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("opener", List.of());
        return new GeneratedProjectPlan(projectId, projectId + "_name", priority, playground, interrogation);
    }
}
