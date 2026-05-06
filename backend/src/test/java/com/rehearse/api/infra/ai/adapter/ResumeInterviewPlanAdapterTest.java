package com.rehearse.api.infra.ai.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
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
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

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
                "plan_test", 2,
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
    @DisplayName("execute_INVALID_PLAN_when_chain_id_missing_separator")
    void execute_throws_invalid_plan_when_chain_id_missing_separator() {
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                "plan_test", 1, List.of(project("p1", 1, "no-separator")));
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
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan("plan_test", 1, List.of(plan));
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        assertThatThrownBy(() -> adapter.execute(request, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    @Test
    @DisplayName("execute_INVALID_PLAN_when_project_plans_null")
    void execute_throws_invalid_plan_when_project_plans_null() {
        GeneratedInterviewPlan raw = new GeneratedInterviewPlan("plan_test", 0, null);
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        assertThatThrownBy(() -> adapter.execute(request, 30))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    @Test
    @DisplayName("execute_with_skeleton_허용되지_않은_chain_id_drop")
    void execute_drops_invalid_chain_ids_when_skeleton_provided() {
        // given
        ResumeSkeleton skeleton = mockSkeleton("p1", Set.of("p1::Redis"));

        GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                "plan_test", 1,
                List.of(projectWithChains("p1", 1,
                        List.of(
                                new GeneratedChainRef("p1::Redis", "Redis", 1, List.of(1, 2)),
                                new GeneratedChainRef("p1::HALLUCINATED", "HALLUCINATED", 2, List.of(1, 2))
                        ),
                        List.of()))
        );
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

        // when
        InterviewPlan result = adapter.execute(request, 30, skeleton);

        // then: 허용된 chain만 남음
        List<?> primaryChains = result.projectPlans().get(0).interrogationPhase().primaryChains();
        assertThat(primaryChains).hasSize(1);
        assertThat(result.projectPlans().get(0).interrogationPhase().primaryChains().get(0).chainId())
                .isEqualTo("p1::Redis");
    }

    @Test
    @DisplayName("execute_with_skeleton_drop_후_primary_chain_없으면_재시도_후_INVALID_PLAN")
    void execute_throws_invalid_plan_after_retry_when_no_valid_chain_remains() {
        // given
        ResumeSkeleton skeleton = mockSkeleton("p1", Set.of("p1::Redis"));
        ChatResponse retryResponse = new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);

        // 첫 번째 LLM 응답: 허용 안된 chain만 있음
        GeneratedInterviewPlan invalidRaw = new GeneratedInterviewPlan(
                "plan_test", 1,
                List.of(projectWithChains("p1", 1,
                        List.of(new GeneratedChainRef("p1::HALLUCINATED", "HALLUCINATED", 1, List.of(1, 2))),
                        List.of()))
        );
        // 재시도 응답도 동일하게 허용 안된 chain
        GeneratedInterviewPlan retryRaw = new GeneratedInterviewPlan(
                "plan_retry", 1,
                List.of(projectWithChains("p1", 1,
                        List.of(new GeneratedChainRef("p1::STILL_WRONG", "STILL_WRONG", 1, List.of(1, 2))),
                        List.of()))
        );

        // setUp의 request-specific stub을 덮어쓰므로 lenient 처리
        lenient().when(aiClient.chat(request)).thenReturn(response);
        given(aiClient.chat(any())).willReturn(response, retryResponse);
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(invalidRaw, retryRaw);

        // when / then
        assertThatThrownBy(() -> adapter.execute(request, 30, skeleton))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.INVALID_PLAN.getCode()));
    }

    @Nested
    @DisplayName("projectName 정합성 - skeleton 우선 강제")
    class ProjectNameReconciliation {

        private ListAppender<ILoggingEvent> logAppender;
        private Logger adapterLogger;

        @BeforeEach
        void attachLogAppender() {
            adapterLogger = (Logger) LoggerFactory.getLogger(ResumeInterviewPlanAdapter.class);
            logAppender = new ListAppender<>();
            logAppender.start();
            adapterLogger.addAppender(logAppender);
        }

        @AfterEach
        void detachLogAppender() {
            adapterLogger.detachAppender(logAppender);
            logAppender.stop();
        }

        @Test
        @DisplayName("skeleton/planner projectName 일치 시 skeleton 값 사용 + WARN 미발생")
        void uses_skeleton_name_and_no_warn_when_names_match() {
            // given
            ResumeSkeleton skeleton = skeletonWithProjectName("p1", "주문 API 캐싱 프로젝트");
            GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                    "plan_test", 1,
                    List.of(projectWithName("p1", 1, "주문 API 캐싱 프로젝트", "p1::Redis"))
            );
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

            // when
            InterviewPlan result = adapter.execute(request, 30, skeleton);

            // then
            assertThat(result.projectPlans().get(0).projectName()).isEqualTo("주문 API 캐싱 프로젝트");
            assertThat(warnMessages()).noneMatch(m -> m.contains("planner projectName mismatch"));
        }

        @Test
        @DisplayName("planner 가 다른 명칭 반환 시 skeleton 값 우선 + WARN 로그 발생 (본문 미노출)")
        void prefers_skeleton_name_and_logs_warn_when_names_mismatch() {
            // given
            ResumeSkeleton skeleton = skeletonWithProjectName("p1", "주문 API 캐싱 프로젝트");
            GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                    "plan_test", 1,
                    List.of(projectWithName("p1", 1, "Order Cache Project", "p1::Redis"))
            );
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

            // when
            InterviewPlan result = adapter.execute(request, 30, skeleton);

            // then
            assertThat(result.projectPlans().get(0).projectName()).isEqualTo("주문 API 캐싱 프로젝트");
            assertThat(warnMessages())
                    .anySatisfy(message -> {
                        assertThat(message).contains("planner projectName mismatch");
                        assertThat(message).contains("projectId=p1");
                        assertThat(message).contains("skeleton 우선");
                        assertThat(message).doesNotContain("주문 API 캐싱 프로젝트");
                        assertThat(message).doesNotContain("Order Cache Project");
                    });
        }

        @Test
        @DisplayName("planner projectName null 시 skeleton 우선 + WARN 미발생")
        void prefers_skeleton_name_silently_when_planner_name_null() {
            // given
            ResumeSkeleton skeleton = skeletonWithProjectName("p1", "주문 API 캐싱 프로젝트");
            GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                    "plan_test", 1,
                    List.of(projectWithName("p1", 1, null, "p1::Redis"))
            );
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

            // when
            InterviewPlan result = adapter.execute(request, 30, skeleton);

            // then
            assertThat(result.projectPlans().get(0).projectName()).isEqualTo("주문 API 캐싱 프로젝트");
            assertThat(warnMessages()).noneMatch(m -> m.contains("planner projectName mismatch"));
        }

        @Test
        @DisplayName("planner projectName blank 시 skeleton 우선 + WARN 미발생")
        void prefers_skeleton_name_silently_when_planner_name_blank() {
            // given
            ResumeSkeleton skeleton = skeletonWithProjectName("p1", "주문 API 캐싱 프로젝트");
            GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                    "plan_test", 1,
                    List.of(projectWithName("p1", 1, "   ", "p1::Redis"))
            );
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

            // when
            InterviewPlan result = adapter.execute(request, 30, skeleton);

            // then
            assertThat(result.projectPlans().get(0).projectName()).isEqualTo("주문 API 캐싱 프로젝트");
            assertThat(warnMessages()).noneMatch(m -> m.contains("planner projectName mismatch"));
        }

        @Test
        @DisplayName("skeleton 미제공 + planner projectName 정상 시 planner 값 사용 (기존 호환)")
        void uses_planner_name_when_skeleton_absent() {
            // given
            GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                    "plan_test", 1,
                    List.of(projectWithName("p1", 1, "Order Cache Project", "p1::Redis"))
            );
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

            // when
            InterviewPlan result = adapter.execute(request, 30);

            // then
            assertThat(result.projectPlans().get(0).projectName()).isEqualTo("Order Cache Project");
        }

        @Test
        @DisplayName("skeleton 미제공 + planner projectName 부재 시 PROJECT_NAME_INVALID (안전망)")
        void throws_invariant_violation_when_both_names_blank() {
            // given
            GeneratedInterviewPlan raw = new GeneratedInterviewPlan(
                    "plan_test", 1,
                    List.of(projectWithName("p1", 1, null, "p1::Redis"))
            );
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any())).willReturn(raw);

            // when / then
            assertThatThrownBy(() -> adapter.execute(request, 30))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ResumeErrorCode.PROJECT_NAME_INVALID.getCode()));
        }

        private List<String> warnMessages() {
            return logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
        }

        private ResumeSkeleton skeletonWithProjectName(String projectId, String projectName) {
            ResumeSkeleton skeleton = mock(ResumeSkeleton.class);
            Project project = mock(Project.class);
            given(skeleton.projects()).willReturn(List.of(project));
            given(project.projectId()).willReturn(projectId);
            given(project.projectName()).willReturn(projectName);

            com.rehearse.api.domain.resume.entity.InterrogationChain chain =
                    mock(com.rehearse.api.domain.resume.entity.InterrogationChain.class);
            given(chain.topic()).willReturn("Redis");
            given(project.implicitCsTopics()).willReturn(List.of(chain));
            return skeleton;
        }

        private GeneratedProjectPlan projectWithName(String projectId, int priority,
                                                     String projectName, String chainId) {
            GeneratedChainRef chain = new GeneratedChainRef(
                    chainId,
                    chainId.contains("::") ? chainId.split("::")[1] : "topic",
                    1,
                    List.of(1, 2));
            GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(chain), List.of());
            GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("opener", List.of());
            return new GeneratedProjectPlan(projectId, projectName, priority, playground, interrogation);
        }
    }

    private ResumeSkeleton mockSkeleton(String projectId, Set<String> allowedChainIds) {
        ResumeSkeleton skeleton = mock(ResumeSkeleton.class);
        Project project = mock(Project.class);
        given(skeleton.projects()).willReturn(List.of(project));
        given(project.projectId()).willReturn(projectId);

        List<com.rehearse.api.domain.resume.entity.InterrogationChain> chains = allowedChainIds.stream()
                .map(chainId -> {
                    String topic = chainId.split("::")[1];
                    com.rehearse.api.domain.resume.entity.InterrogationChain chain =
                            mock(com.rehearse.api.domain.resume.entity.InterrogationChain.class);
                    given(chain.topic()).willReturn(topic);
                    return chain;
                })
                .toList();
        given(project.implicitCsTopics()).willReturn(chains);
        return skeleton;
    }

    private GeneratedProjectPlan project(String projectId, int priority, String chainId) {
        GeneratedChainRef chain = new GeneratedChainRef(chainId, chainId.contains("::") ? chainId.split("::")[1] : "topic", 1, List.of(1, 2));
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(List.of(chain), List.of());
        GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("opener", List.of());
        return new GeneratedProjectPlan(projectId, projectId + "_name", priority, playground, interrogation);
    }

    private GeneratedProjectPlan projectWithChains(String projectId, int priority,
                                                    List<GeneratedChainRef> primary,
                                                    List<GeneratedChainRef> backup) {
        GeneratedInterrogationPhase interrogation = new GeneratedInterrogationPhase(primary, backup);
        GeneratedPlaygroundPhase playground = new GeneratedPlaygroundPhase("opener", List.of());
        return new GeneratedProjectPlan(projectId, projectId + "_name", priority, playground, interrogation);
    }
}
