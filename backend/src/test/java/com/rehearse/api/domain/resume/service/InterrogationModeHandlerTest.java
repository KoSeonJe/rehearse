package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterrogationModeHandler - Interrogation 모드 결정 트리")
class InterrogationModeHandlerTest {

    @InjectMocks
    private InterrogationModeHandler handler;

    @Mock
    private ResumeChainInterrogatorPromptBuilder promptBuilder;

    @Mock
    private ResumeQuestionPersister questionPersister;

    private InterviewRuntimeState state;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        plan = createPlan();
        lenient().when(questionPersister.persist(anyLong(), any(), any(), anyInt()))
                .thenReturn(1L);
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

            InterrogationModeHandler.InterrogationTurnResult result = handler.handle(1L, state, "좋은 답변", createAnalysis(4), plan);
            FollowUpResponse response = result.response();

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

            InterrogationModeHandler.InterrogationTurnResult result = handler.handle(1L, state, "답변", createAnalysis(3), plan);

            assertThat(result.response().isFollowUpExhausted()).isTrue();
            assertThat(result.response().isPresentToUser()).isFalse();
        }
    }

    @Nested
    @DisplayName("LLM 응답 검증")
    class LlmResponseValidation {

        @Test
        @DisplayName("LLM 이 빈 question 을 반환하면 BusinessException(RESPONSE_INVALID) 을 던진다")
        void handle_blankQuestion_throwsBusinessException() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("", "", "이유", "LEVEL_STAY", 1));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(3), plan))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));
        }

        @Test
        @DisplayName("LLM 이 null question 을 반환하면 BusinessException(RESPONSE_INVALID) 을 던진다")
        void handle_nullQuestion_throwsBusinessException() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(promptBuilder.build(any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult(null, null, "이유", "LEVEL_STAY", 1));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(3), plan))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));
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
        return new InterviewPlan("plan-001", List.of(projectPlan));
    }
}
