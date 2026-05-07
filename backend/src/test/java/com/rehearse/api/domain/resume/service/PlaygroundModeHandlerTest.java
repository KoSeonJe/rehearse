package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.EvidenceStrength;
import com.rehearse.api.domain.interview.entity.Perspective;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.InterrogationChain;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ChainStep;
import com.rehearse.api.domain.resume.entity.StepType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.domain.resume.service.ResumeQuestionResultGenerator;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult.SwitchConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaygroundModeHandler - Playground 모드 처리")
class PlaygroundModeHandlerTest {

    @InjectMocks
    private PlaygroundModeHandler handler;

    @Mock
    private ResumeQuestionResultGenerator resultGenerator;

    @Mock
    private ResumeQuestionPersister questionPersister;

    @Mock
    private ResumeModeTransitionPolicy modeTransitionPolicy;

    private InterviewRuntimeState state;
    private ResumeSkeleton skeleton;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        skeleton = createSkeleton();
        plan = createPlan();
        Mockito.lenient()
                .when(questionPersister.persist(anyLong(), any(), any(), any(), any(), anyInt()))
                .thenReturn(1L);
        Mockito.lenient()
                .when(modeTransitionPolicy.isPlaygroundHardCapReached(anyInt()))
                .thenReturn(false);
    }

    @Nested
    @DisplayName("4 전환 조건 평가")
    class SwitchConditionEvaluation {

        @Test
        @DisplayName("4조건 중 2개 충족(a+b) 시 switchedToInterrogation=true 를 반환한다")
        void handle_conditionsAB_met_switches() {
            SwitchConditions cond = new SwitchConditions(true, true, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond, "model"));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변입니다", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.switchedToInterrogation()).isTrue();
            assertThat(result.questionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("4조건 중 1개만 충족 시 switchedToInterrogation=false 를 반환한다")
        void handle_only1Condition_doesNotSwitch() {
            SwitchConditions cond = new SwitchConditions(true, false, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond, "model"));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "짧은 답변", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.switchedToInterrogation()).isFalse();
            assertThat(result.questionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("shouldSwitchToInterrogation=true 이면 조건 count 무관하게 전환된다")
        void handle_explicitSwitch_alwaysTransitions() {
            SwitchConditions cond = new SwitchConditions(false, false, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult(null, null, "이유", true, cond, "model"));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.switchedToInterrogation()).isTrue();
        }

        @Test
        @DisplayName("d 조건(3턴 이상)만 충족 시 단독으로는 전환하지 않는다")
        void handle_onlyTurnLimit_doesNotSwitch() {
            SwitchConditions cond = new SwitchConditions(false, false, false, true);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond, "model"));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.switchedToInterrogation()).isFalse();
            assertThat(result.questionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("c+d 조건 충족 시 전환된다")
        void handle_conditionsCD_met_switches() {
            SwitchConditions cond = new SwitchConditions(false, false, true, true);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond, "model"));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "그래서 결론적으로 정리하면", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.switchedToInterrogation()).isTrue();
            assertThat(result.questionId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Playground 오프너 생성")
    class OpenerGeneration {

        @Test
        @DisplayName("handleOpener 호출 시 playgroundTurns 가 1 증가하고 응답을 반환한다")
        void handleOpener_incrementsTurnsAndReturnsResponse() {
            given(resultGenerator.generateOpener(any(), any(), any(), any()))
                    .willReturn(new PlaygroundOpenerResult("Redis 프로젝트 소개해주세요", "Redis 프로젝트 소개해주세요", "오프너", "model"));
            given(questionPersister.persist(anyLong(), any(), any(), any(), any(), anyInt()))
                    .willReturn(1L);

            PlaygroundModeHandler.OpenerResult result = handler.handleOpener(1L, state, skeleton, plan);

            assertThat(result.response().getQuestion()).isEqualTo("Redis 프로젝트 소개해주세요");
            assertThat(state.getPlaygroundTurns().get()).isEqualTo(1);
            assertThat(result.response().isSkip()).isFalse();
            assertThat(result.response().isPresentToUser()).isTrue();
            assertThat(result.questionId()).isEqualTo(1L);
        }

    }

    @Nested
    @DisplayName("projectName 컨텍스트 주입")
    class ProjectNameInjection {

        @Test
        @DisplayName("handleOpener 가 buildOpener 에 skeleton 의 Project (projectName 포함) 를 전달한다")
        void handleOpener_passesProjectWithProjectName() {
            given(resultGenerator.generateOpener(any(), any(), any(), any()))
                    .willReturn(new PlaygroundOpenerResult("Redis 캐싱 프로젝트에서 어떤 역할이었나요?",
                            "Redis 캐싱 프로젝트에서 어떤 역할이었나요?", "오프너", "model"));

            handler.handleOpener(1L, state, skeleton, plan);

            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            then(resultGenerator).should().generateOpener(any(), any(), projectCaptor.capture(), any());
            assertThat(projectCaptor.getValue().projectName()).isEqualTo("Redis 캐싱 프로젝트");
        }

        @Test
        @DisplayName("handle 가 buildResponder 에 skeleton 의 Project (projectName 포함) 를 전달한다")
        void handle_passesProjectWithProjectName() {
            SwitchConditions cond = new SwitchConditions(false, false, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond, "model"));

            handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan, List.of());

            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            then(resultGenerator).should().generatePlaygroundResponder(any(), any(), any(), projectCaptor.capture(),
                    any(), any(), anyInt(), anyInt());
            assertThat(projectCaptor.getValue().projectName()).isEqualTo("Redis 캐싱 프로젝트");
        }
    }

    @Nested
    @DisplayName("응답 DTO questionId 주입 (Issue #433 회귀 가드)")
    class ResponseQuestionIdInjection {

        @Test
        @DisplayName("handleOpener 응답 DTO 가 persister 반환 questionId 를 보유한다")
        void handleOpener_responseCarriesPersistedQuestionId() {
            given(resultGenerator.generateOpener(any(), any(), any(), any()))
                    .willReturn(new PlaygroundOpenerResult("Redis 프로젝트 소개해주세요", "Redis 프로젝트 소개해주세요", "오프너", "model"));
            given(questionPersister.persist(anyLong(), any(), any(), any(), any(), anyInt()))
                    .willReturn(777L);

            PlaygroundModeHandler.OpenerResult result = handler.handleOpener(1L, state, skeleton, plan);

            assertThat(result.questionId()).isEqualTo(777L);
            assertThat(result.response().getQuestionId())
                    .as("응답 DTO 가 자기 질문 ID 를 보유해야 FE 매핑 정상")
                    .isEqualTo(777L);
        }

        @Test
        @DisplayName("handle (responder) 응답 DTO 가 persister 반환 questionId 를 보유한다")
        void handle_responderResponseCarriesPersistedQuestionId() {
            SwitchConditions cond = new SwitchConditions(false, false, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("후속 질문", "후속 질문", "이유", false, cond, "model"));
            given(questionPersister.persist(anyLong(), any(), any(), any(), any(), anyInt()))
                    .willReturn(888L);

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.questionId()).isEqualTo(888L);
            assertThat(result.response().getQuestionId())
                    .as("응답 DTO questionId 와 result.questionId 정합")
                    .isEqualTo(888L);
        }
    }

    @Nested
    @DisplayName("Responder 빈 question 처리")
    class ResponderBlankQuestion {

        @Test
        @DisplayName("shouldSwitch=true 이고 question 이 blank 이면 persist 를 호출하지 않고 정상 응답한다")
        void handle_blankQuestion_withSwitch_skipsPersistAndReturnsResponse() {
            SwitchConditions cond = new SwitchConditions(false, false, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult(null, null, "전환 이유", true, cond, "model"));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan, List.of());

            assertThat(result.switchedToInterrogation()).isTrue();
            assertThat(result.questionId()).isNull();
        }

        @Test
        @DisplayName("shouldSwitch=false 이고 question 이 blank 이면 BusinessException(RESPONSE_INVALID) 을 던진다")
        void handle_blankQuestion_withoutSwitch_throwsBusinessException() {
            SwitchConditions cond = new SwitchConditions(false, false, false, false);
            given(resultGenerator.generatePlaygroundResponder(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("", "", "이유", false, cond, "model"));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan, List.of()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));
        }
    }

    private AnswerAnalysis createAnalysis() {
        return new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }

    private ResumeSkeleton createSkeleton() {
        ChainStep what = new ChainStep(1, StepType.WHAT, "Redis 란?");
        ChainStep how = new ChainStep(2, StepType.HOW, "어떻게 사용했나?");
        ChainStep whyMech = new ChainStep(3, StepType.WHY_MECH, "왜 Redis 인가?");
        ChainStep tradeoff = new ChainStep(4, StepType.TRADEOFF, "트레이드오프는?");
        InterrogationChain chain = new InterrogationChain("Redis 캐싱", 0.9, List.of(what, how, whyMech, tradeoff));
        Project project = new Project("proj1", "Redis 캐싱 프로젝트", List.of(), List.of(chain));
        return new ResumeSkeleton("resume1", "hash123", null, "backend", List.of(project), Map.of());
    }

    private InterviewPlan createPlan() {
        ChainReference chainRef = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요", List.of("Redis 사용", "캐싱 전략"));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chainRef), List.of());
        ProjectPlan projectPlan = new ProjectPlan("proj1", "Redis Cache", 1, playground, interrogation);
        return new InterviewPlan("plan-001", List.of(projectPlan));
    }
}
