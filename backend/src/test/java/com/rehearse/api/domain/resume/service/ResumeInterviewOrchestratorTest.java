package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.service.AnswerAnalyzer;
import com.rehearse.api.domain.interview.service.IntentBranchInput;
import com.rehearse.api.domain.interview.service.IntentClassifier;
import com.rehearse.api.domain.interview.service.IntentDispatcher;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import org.springframework.context.ApplicationEventPublisher;
import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeInterviewOrchestrator - FSM 라우팅")
class ResumeInterviewOrchestratorTest {

    @InjectMocks
    private ResumeInterviewOrchestrator orchestrator;

    @Mock
    private IntentClassifier intentClassifier;
    @Mock
    private AnswerAnalyzer answerAnalyzer;
    @Mock
    private IntentDispatcher intentDispatcher;
    @Mock
    private PlaygroundModeHandler playgroundHandler;
    @Mock
    private InterrogationModeHandler interrogationHandler;
    @Mock
    private WrapUpModeHandler wrapUpHandler;
    @Mock
    private ClockWatcher clockWatcher;
    @Mock
    private InterviewRuntimeStateCache runtimeStateStore;
    @Mock
    private InterviewFinder interviewFinder;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InterviewRuntimeState state;
    private ResumeSkeleton skeleton;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        skeleton = new ResumeSkeleton("r1", "h1", null, "backend", List.of(), Map.of());
        plan = createPlan();

        ReflectionTestUtils.setField(orchestrator, "wrapUpThresholdMin", 2L);
        ReflectionTestUtils.setField(orchestrator, "hardTimeoutMin", 10L);

        lenient().when(runtimeStateStore.get(anyLong())).thenReturn(state);
        lenient().doAnswer(inv -> {
            java.util.function.Consumer<InterviewRuntimeState> mutator = inv.getArgument(1);
            mutator.accept(state);
            return null;
        }).when(runtimeStateStore).update(anyLong(), any());
    }

    @Nested
    @DisplayName("Intent 분기")
    class IntentBranching {

        @Test
        @DisplayName("intent=ANSWER 이면 현재 mode 핸들러로 라우팅된다")
        void processUserTurn_answer_routesToModeHandler() {
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.95, "answer"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(createAnalysis());
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false));

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("Q");
            then(playgroundHandler).should().handle(any(), any(), any(), any(), any(), any());
            then(intentDispatcher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("intent=CLARIFY_REQUEST 이면 intentDispatcher 로 단축되고 question 을 반환한다")
        void processUserTurn_clarify_dispatchesToIntentDispatcher() {
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.CLARIFY_REQUEST, 0.9, "clarify"));
            given(intentDispatcher.dispatch(eq(IntentType.CLARIFY_REQUEST), any()))
                    .willReturn(FollowUpResponse.builder().question("재설명 질문").build());

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "이 질문이 무슨 뜻인가요?", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("재설명 질문");
            then(intentDispatcher).should().dispatch(eq(IntentType.CLARIFY_REQUEST), any());
            then(answerAnalyzer).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("intent=GIVE_UP 이면 intentDispatcher 로 단축되고 followUpExhausted=false 를 반환한다")
        void processUserTurn_giveUp_dispatchesToIntentDispatcher() {
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.GIVE_UP, 0.92, "giveup"));
            given(intentDispatcher.dispatch(eq(IntentType.GIVE_UP), any()))
                    .willReturn(FollowUpResponse.builder().question("힌트 제공").followUpExhausted(false).build());

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "모르겠습니다", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("힌트 제공");
            assertThat(response.isFollowUpExhausted()).isFalse();
            then(intentDispatcher).should().dispatch(eq(IntentType.GIVE_UP), any());
        }
    }

    @Nested
    @DisplayName("WRAP_UP 자동 전이")
    class WrapUpTransition {

        @BeforeEach
        void wrapUpSetUp() {
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.9, "answer"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(createAnalysis());
        }

        @Test
        @DisplayName("remainingMinutes <= wrapUpThresholdMin 이면 WRAP_UP 으로 전이된다")
        void processUserTurn_timeRunsOut_transitionsToWrapUp() {
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(1L);
            given(wrapUpHandler.handle(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(FollowUpResponse.builder().question("마무리").presentToUser(true).build());

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(state.getResumeMode()).isEqualTo(ResumeMode.WRAP_UP);
            assertThat(response.getQuestion()).isEqualTo("마무리");
            then(wrapUpHandler).should().handle(any(), any(), any(), any(), anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("이미 WRAP_UP 모드이면 추가 전이를 시도하지 않는다")
        void processUserTurn_alreadyWrapUp_noDoubleTransition() {
            state.transitionTo(ResumeMode.WRAP_UP);
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(1L);
            given(wrapUpHandler.handle(any(), any(), any(), any(), anyLong(), anyBoolean()))
                    .willReturn(FollowUpResponse.builder().question("마무리").build());

            orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(state.getResumeMode()).isEqualTo(ResumeMode.WRAP_UP);
        }
    }

    @Nested
    @DisplayName("모드별 라우팅")
    class ModeRouting {

        @Test
        @DisplayName("mode=INTERROGATION 이면 interrogationHandler 로 라우팅된다")
        void processUserTurn_interrogationMode_routesToInterrogationHandler() {
            state.transitionTo(ResumeMode.INTERROGATION);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.9, "answer"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(createAnalysis());
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(interrogationHandler.handle(any(), any(), any(), any(), any()))
                    .willReturn(FollowUpResponse.builder().question("L2 질문").build());

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("L2 질문");
            then(interrogationHandler).should().handle(any(), any(), any(), any(), any());
            then(playgroundHandler).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("세션 시작")
    class SessionStart {

        @Test
        @DisplayName("startSession 호출 시 clockWatcher.markStart 가 호출되고 Playground opener 를 반환한다")
        void startSession_marksStartAndReturnsOpener() {
            given(playgroundHandler.handleOpener(anyLong(), any(), any(), any()))
                    .willReturn(FollowUpResponse.builder()
                            .question("Redis 프로젝트를 소개해주세요")
                            .presentToUser(true)
                            .type("RESUME_PLAYGROUND")
                            .build());

            FollowUpResponse response = orchestrator.startSession(1L, 30, skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("Redis 프로젝트를 소개해주세요");
            assertThat(response.isPresentToUser()).isTrue();
            then(clockWatcher).should().markStart(1L);
            then(playgroundHandler).should().handleOpener(eq(1L), any(), eq(skeleton), eq(plan));
        }
    }

    @Nested
    @DisplayName("TurnCompletedEvent payload")
    class TurnCompletedEventPayload {

        @Test
        @DisplayName("processUserTurn 후 발행된 이벤트의 userAnswer, resumeMode, currentChainLevel이 올바르게 채워진다")
        void processUserTurn_publishes_event_with_correct_payload() {
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.95, "answer"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(createAnalysis());
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false));

            Interview interview = Interview.builder()
                    .userId(99L)
                    .position(Position.BACKEND)
                    .level(InterviewLevel.MID)
                    .interviewTypes(List.of(InterviewType.RESUME_BASED))
                    .durationMinutes(30)
                    .build();
            given(interviewFinder.findById(1L)).willReturn(interview);

            orchestrator.processUserTurn(1L, 30, "질문텍스트", "사용자답변텍스트", List.of(), skeleton, plan);

            ArgumentCaptor<TurnCompletedEvent> captor = ArgumentCaptor.forClass(TurnCompletedEvent.class);
            then(eventPublisher).should().publishEvent(captor.capture());

            TurnCompletedEvent captured = captor.getValue();
            assertThat(captured.userAnswer()).isEqualTo("사용자답변텍스트");
            assertThat(captured.resumeMode()).isEqualTo(ResumeMode.PLAYGROUND);
            assertThat(captured.currentChainLevel()).isNotNull();
            assertThat(captured.interviewId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("interviewFinder 예외 시 event 발행 실패해도 응답은 정상 반환된다")
        void processUserTurn_eventPublishFailure_doesNotBlockResponse() {
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.95, "answer"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(createAnalysis());
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false));
            given(interviewFinder.findById(anyLong()))
                    .willThrow(new RuntimeException("DB 오류"));

            FollowUpResponse response = orchestrator.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("Q");
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    private AnswerAnalysis createAnalysis() {
        return new AnswerAnalysis(1L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
    }

    private InterviewPlan createPlan() {
        ChainReference chain = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase playground = new PlaygroundPhase("소개해주세요", List.of());
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        ProjectPlan projectPlan = new ProjectPlan("proj1", "Redis", 1, playground, interrogation);
        return new InterviewPlan("plan-001", 30, List.of(projectPlan));
    }
}
