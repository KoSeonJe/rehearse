package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.InterviewPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterrogationModeHandler - Interrogation 모드 결정 트리")
class InterrogationModeHandlerTest {

    @InjectMocks
    private InterrogationModeHandler handler;

    @Mock
    private ResumeChainInterrogatorPromptBuilder promptBuilder;

    private InterviewRuntimeState state;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        plan = createPlan();
    }

    @Nested
    @DisplayName("LEVEL_UP 결정")
    class LevelUp {

        @Test
        @DisplayName("answer_quality >= 3 AND level < 4 이면 레벨이 올라간다")
        void handle_highQuality_levelsUp() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("L2 질문", "L2 질문", "이유", "LEVEL_UP", 2));

            FollowUpResponse response = handler.handle(1L, state, "좋은 답변", createAnalysis(4), plan);

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(2);
            assertThat(response.getType()).startsWith("RESUME_INTERROGATION_L");
            assertThat(response.isSkip()).isFalse();
        }
    }

    @Nested
    @DisplayName("LEVEL_STAY 결정")
    class LevelStay {

        @Test
        @DisplayName("answer_quality <= 2 이면 같은 레벨을 유지한다")
        void handle_lowQuality_staysAtSameLevel() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("재질문", "재질문", "이유", "LEVEL_STAY", 1));

            handler.handle(1L, state, "모호한 답변", createAnalysis(2), plan);

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(1);
            assertThat(state.getChainStateTracker().getConsecutiveLevelStayCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("LEVEL_STAY 연속 3회(2턴 초과) 시 강제 LEVEL_UP 된다")
        void handle_levelStayExceeded_forcesLevelUp() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            state.getChainStateTracker().levelStay();
            state.getChainStateTracker().levelStay();

            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("재질문", "재질문", "이유", "LEVEL_STAY", 1));

            handler.handle(1L, state, "또 모호한 답변", createAnalysis(1), plan);

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("level=4 에서 LEVEL_STAY 한계 초과 시 chain 이 완료 처리된다")
        void handle_levelStayExceededAtMax_completesChain() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            state.getChainStateTracker().levelUp();
            state.getChainStateTracker().levelUp();
            state.getChainStateTracker().levelUp();
            state.getChainStateTracker().levelStay();
            state.getChainStateTracker().levelStay();

            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("재질문", "재질문", "이유", "LEVEL_STAY", 4));

            handler.handle(1L, state, "또 모호한 답변", createAnalysis(1), plan);

            assertThat(state.getChainStateTracker().getCompletedChainIds()).contains("proj1::redis");
        }
    }

    @Nested
    @DisplayName("CHAIN_SWITCH 결정")
    class ChainSwitch {

        @Test
        @DisplayName("CHAIN_SWITCH 결정 시 현재 chain 이 완료 처리된다")
        void handle_chainSwitch_completesCurrentChain() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("다음 주제", "다음 주제", "이유", "CHAIN_SWITCH", 1));

            handler.handle(1L, state, "모릅니다", createAnalysis(1), plan);

            assertThat(state.getChainStateTracker().getCompletedChainIds()).contains("proj1::redis");
            assertThat(state.getChainStateTracker().hasActiveChain()).isFalse();
        }
    }

    @Nested
    @DisplayName("Chain 소진")
    class ChainExhaustion {

        @Test
        @DisplayName("모든 chain 소진 시 followUpExhausted=true 응답을 반환한다")
        void handle_allChainsExhausted_returnsExhausted() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            state.getChainStateTracker().markChainComplete();

            FollowUpResponse response = handler.handle(1L, state, "답변", createAnalysis(3), plan);

            assertThat(response.isFollowUpExhausted()).isTrue();
            assertThat(response.isPresentToUser()).isFalse();
        }
    }

    private AnswerAnalysis createAnalysis(int quality) {
        return new AnswerAnalysis(1L, List.of(), List.of(), List.of(), quality, RecommendedNextAction.DEEP_DIVE);
    }

    private InterviewPlan createPlan() {
        ChainReference primary = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트 소개해주세요", List.of());
        InterrogationPhase interrogation = new InterrogationPhase(List.of(primary), List.of());
        ProjectPlan projectPlan = new ProjectPlan("proj1", "Redis Cache", 1, playground, interrogation);
        return new InterviewPlan("plan-001", 30, List.of(projectPlan));
    }
}
