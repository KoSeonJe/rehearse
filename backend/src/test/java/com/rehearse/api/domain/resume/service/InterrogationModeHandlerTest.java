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
import com.rehearse.api.domain.resume.service.ResumeQuestionResultGenerator;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterrogationModeHandler - Interrogation 모드 결정 트리")
class InterrogationModeHandlerTest {

    @InjectMocks
    private InterrogationModeHandler handler;

    @Mock
    private ResumeQuestionResultGenerator resultGenerator;

    @Mock
    private ResumeQuestionPersister questionPersister;

    private InterviewRuntimeState state;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        plan = createPlan();
        lenient().when(questionPersister.persist(anyLong(), any(), any(), any(), any(), anyInt()))
                .thenReturn(1L);
    }

    @Nested
    @DisplayName("LEVEL_UP 결정")
    class LevelUp {

        @Test
        @DisplayName("answer_quality >= 3 AND level < 4 이면 레벨이 올라간다")
        void handle_highQuality_levelsUp() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("L2 질문", "L2 질문", "이유", "LEVEL_UP", 2, "model"));

            InterrogationTurnResult result = handler.handle(1L, state, "좋은 답변", createAnalysis(4), plan, java.util.List.of());
            FollowUpResponse response = result.response();

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(2);
            assertThat(response.getType()).startsWith("RESUME_INTERROGATION_L");
            assertThat(response.isSkip()).isFalse();
            assertThat(result.questionId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("LEVEL_STAY 결정")
    class LevelStay {

        @Test
        @DisplayName("answer_quality <= 2 이면 같은 레벨을 유지한다")
        void handle_lowQuality_staysAtSameLevel() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("재질문", "재질문", "이유", "LEVEL_STAY", 1, "model"));

            handler.handle(1L, state, "모호한 답변", createAnalysis(2), plan, java.util.List.of());

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(1);
            assertThat(state.getChainStateTracker().getConsecutiveLevelStayCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("LEVEL_STAY 연속 3회(2턴 초과) 시 강제 LEVEL_UP 된다")
        void handle_levelStayExceeded_forcesLevelUp() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            state.getChainStateTracker().levelStay();
            state.getChainStateTracker().levelStay();

            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("재질문", "재질문", "이유", "LEVEL_STAY", 1, "model"));

            handler.handle(1L, state, "또 모호한 답변", createAnalysis(1), plan, java.util.List.of());

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

            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("재질문", "재질문", "이유", "LEVEL_STAY", 4, "model"));

            handler.handle(1L, state, "또 모호한 답변", createAnalysis(1), plan, java.util.List.of());

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
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("다음 주제", "다음 주제", "이유", "CHAIN_SWITCH", 1, "model"));

            handler.handle(1L, state, "모릅니다", createAnalysis(1), plan, java.util.List.of());

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

            InterrogationTurnResult result = handler.handle(1L, state, "답변", createAnalysis(3), plan, java.util.List.of());

            assertThat(result.response().isFollowUpExhausted()).isTrue();
            assertThat(result.response().isPresentToUser()).isFalse();
            assertThat(result.questionId()).isNull();
        }
    }

    @Nested
    @DisplayName("LLM 응답 검증")
    class LlmResponseValidation {

        @Test
        @DisplayName("LLM 이 빈 question 을 반환하면 BusinessException(RESPONSE_INVALID) 을 lock 밖에서 던지고 tracker state 가 변하지 않는다")
        void handle_blankQuestion_throwsBusinessException_andDoesNotMutateTracker() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            int levelBefore = state.getChainStateTracker().getCurrentLevel();
            int stayBefore = state.getChainStateTracker().getConsecutiveLevelStayCount();
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("", "", "이유", "LEVEL_STAY", 1, "model"));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(3), plan, java.util.List.of()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(levelBefore);
            assertThat(state.getChainStateTracker().getConsecutiveLevelStayCount()).isEqualTo(stayBefore);
            verify(questionPersister, never()).persist(anyLong(), any(), any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("LLM 이 null question 을 반환하면 BusinessException(RESPONSE_INVALID) 을 lock 밖에서 던지고 tracker state 가 변하지 않는다")
        void handle_nullQuestion_throwsBusinessException_andDoesNotMutateTracker() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            int levelBefore = state.getChainStateTracker().getCurrentLevel();
            int stayBefore = state.getChainStateTracker().getConsecutiveLevelStayCount();
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult(null, null, "이유", "LEVEL_STAY", 1, "model"));

            assertThatThrownBy(() -> handler.handle(1L, state, "답변", createAnalysis(3), plan, java.util.List.of()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo("AI_007"));

            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(levelBefore);
            assertThat(state.getChainStateTracker().getConsecutiveLevelStayCount()).isEqualTo(stayBefore);
            verify(questionPersister, never()).persist(anyLong(), any(), any(), any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("projectName 컨텍스트 주입")
    class ProjectNameInjection {

        @Test
        @DisplayName("build 호출 시 plan 에서 currentProjectId 에 매칭되는 ProjectPlan.projectName 을 전달한다")
        void handle_passesProjectNameFromPlan() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("L2 질문", "L2 질문", "이유", "LEVEL_UP", 2, "model"));

            handler.handle(1L, state, "답변", createAnalysis(4), plan, java.util.List.of());

            ArgumentCaptor<String> projectNameCaptor = ArgumentCaptor.forClass(String.class);
            then(resultGenerator).should().generateInterrogation(any(), any(), any(),
                    projectNameCaptor.capture(),
                    any(), anyInt(), anyInt(), any(), anyInt());
            assertThat(projectNameCaptor.getValue()).isEqualTo("Redis Cache");
        }
    }

    @Nested
    @DisplayName("Lock 경계 (P1-3)")
    class LockBoundary {

        @Test
        @DisplayName("Phase 2 (LLM 호출) 동안 lock 이 점유되지 않아 다른 thread 의 withLock 이 1초 이내 진입한다")
        void phase2_doesNotHoldLock_allowsOtherThreadToAcquire() throws Exception {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");

            CountDownLatch llmEntered = new CountDownLatch(1);
            CountDownLatch llmRelease = new CountDownLatch(1);
            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willAnswer(inv -> {
                        llmEntered.countDown();
                        // LLM 응답 지연 시뮬: 다른 thread 가 lock 진입할 시간을 벌어준다
                        llmRelease.await(2, TimeUnit.SECONDS);
                        return new InterrogationResult("L2 질문", "L2 질문", "이유", "LEVEL_UP", 2, "model");
                    });

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<InterrogationTurnResult> handlerFuture =
                        executor.submit(() -> handler.handle(1L, state, "답변", createAnalysis(4), plan, java.util.List.of()));

                // LLM 진입까지 대기 = handler 가 Phase 2 안에 있다는 의미 (Phase 1 lock 은 이미 해제)
                assertThat(llmEntered.await(2, TimeUnit.SECONDS)).isTrue();

                AtomicBoolean acquired = new AtomicBoolean(false);
                Future<?> probeFuture = executor.submit(() ->
                        state.getChainStateTracker().withLock(() -> {
                            acquired.set(true);
                            return null;
                        })
                );
                probeFuture.get(1, TimeUnit.SECONDS);
                assertThat(acquired).isTrue();

                // handler 마무리
                llmRelease.countDown();
                InterrogationTurnResult result = handlerFuture.get(2, TimeUnit.SECONDS);
                assertThat(result.questionId()).isEqualTo(1L);
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("persist 는 Phase 3 (lock 안) 에서 tracker state 변경 전에 호출된다 — orphan question row 방지")
        void persist_runsInsideLock_beforeTrackerMutation() {
            state.getChainStateTracker().initChain("proj1", "proj1::redis");
            int levelBeforeTurn = state.getChainStateTracker().getCurrentLevel();

            given(resultGenerator.generateInterrogation(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyInt()))
                    .willReturn(new InterrogationResult("L2 질문", "L2 질문", "이유", "LEVEL_UP", 2, "model"));

            AtomicInteger levelAtPersist = new AtomicInteger(-1);
            given(questionPersister.persist(anyLong(), any(), any(), any(), any(), anyInt()))
                    .willAnswer(inv -> {
                        // persist 호출 시점에 tracker 가 아직 mutation 안 된 상태인지 캡처
                        levelAtPersist.set(state.getChainStateTracker().getCurrentLevel());
                        return 1L;
                    });

            handler.handle(1L, state, "좋은 답변", createAnalysis(4), plan, java.util.List.of());

            // persist 시점 = applyDecision 직전 (level=1) 이어야 한다.
            // applyDecision 후 호출되었다면 levelAtPersist=2 가 되어 fail.
            assertThat(levelAtPersist.get())
                    .as("persist 는 tracker mutation 전에 호출되어야 한다 (Phase 3 lock 안 원자 묶음)")
                    .isEqualTo(levelBeforeTurn);
            assertThat(state.getChainStateTracker().getCurrentLevel()).isEqualTo(2);
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
