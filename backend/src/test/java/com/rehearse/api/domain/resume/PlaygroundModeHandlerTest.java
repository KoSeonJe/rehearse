package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.InterrogationChain;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.Project;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.ChainStep;
import com.rehearse.api.domain.resume.domain.StepType;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult.SwitchConditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private ResumePlaygroundPromptBuilder promptBuilder;

    private InterviewRuntimeState state;
    private ResumeSkeleton skeleton;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        skeleton = createSkeleton();
        plan = createPlan();
    }

    @Nested
    @DisplayName("4 전환 조건 평가")
    class SwitchConditionEvaluation {

        @Test
        @DisplayName("4조건 중 2개 충족(a+b) 시 switchedToInterrogation=true 를 반환한다")
        void handle_conditionsAB_met_switches() {
            SwitchConditions cond = new SwitchConditions(true, true, false, false);
            given(promptBuilder.buildResponder(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변입니다", createAnalysis(), skeleton, plan);

            assertThat(result.switchedToInterrogation()).isTrue();
        }

        @Test
        @DisplayName("4조건 중 1개만 충족 시 switchedToInterrogation=false 를 반환한다")
        void handle_only1Condition_doesNotSwitch() {
            SwitchConditions cond = new SwitchConditions(true, false, false, false);
            given(promptBuilder.buildResponder(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "짧은 답변", createAnalysis(), skeleton, plan);

            assertThat(result.switchedToInterrogation()).isFalse();
        }

        @Test
        @DisplayName("shouldSwitchToInterrogation=true 이면 조건 count 무관하게 전환된다")
        void handle_explicitSwitch_alwaysTransitions() {
            SwitchConditions cond = new SwitchConditions(false, false, false, false);
            given(promptBuilder.buildResponder(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult(null, null, "이유", true, cond));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan);

            assertThat(result.switchedToInterrogation()).isTrue();
        }

        @Test
        @DisplayName("d 조건(3턴 이상)만 충족 시 단독으로는 전환하지 않는다")
        void handle_onlyTurnLimit_doesNotSwitch() {
            SwitchConditions cond = new SwitchConditions(false, false, false, true);
            given(promptBuilder.buildResponder(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "답변", createAnalysis(), skeleton, plan);

            assertThat(result.switchedToInterrogation()).isFalse();
        }

        @Test
        @DisplayName("c+d 조건 충족 시 전환된다")
        void handle_conditionsCD_met_switches() {
            SwitchConditions cond = new SwitchConditions(false, false, true, true);
            given(promptBuilder.buildResponder(any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(new PlaygroundResponderResult("질문", "질문", "이유", false, cond));

            PlaygroundModeHandler.PlaygroundTurnResult result =
                    handler.handle(1L, state, "그래서 결론적으로 정리하면", createAnalysis(), skeleton, plan);

            assertThat(result.switchedToInterrogation()).isTrue();
        }
    }

    @Nested
    @DisplayName("Playground 오프너 생성")
    class OpenerGeneration {

        @Test
        @DisplayName("handleOpener 호출 시 playgroundTurns 가 1 증가하고 응답을 반환한다")
        void handleOpener_incrementsTurnsAndReturnsResponse() {
            given(promptBuilder.buildOpener(any(), any(), any()))
                    .willReturn(new PlaygroundOpenerResult("Redis 프로젝트 소개해주세요", "Redis 프로젝트 소개해주세요", "오프너"));

            FollowUpResponse response = handler.handleOpener(1L, state, skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("Redis 프로젝트 소개해주세요");
            assertThat(state.getPlaygroundTurns().get()).isEqualTo(1);
            assertThat(response.isSkip()).isFalse();
            assertThat(response.isPresentToUser()).isTrue();
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
        Project project = new Project("proj1", List.of(), List.of(chain));
        return new ResumeSkeleton("resume1", "hash123", null, "backend", List.of(project), Map.of());
    }

    private InterviewPlan createPlan() {
        ChainReference chainRef = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요", List.of("Redis 사용", "캐싱 전략"));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chainRef), List.of());
        ProjectPlan projectPlan = new ProjectPlan("proj1", "Redis Cache", 1, playground, interrogation);
        return new InterviewPlan("plan-001", 30, List.of(projectPlan));
    }
}
