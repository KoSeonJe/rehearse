package com.rehearse.api.domain.resume.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.service.TurnAnalysisPipeline;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private TurnAnalysisPipeline turnAnalysisPipeline;
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
    private ResumeModeTransitionPolicy modeTransitionPolicy;
    @Mock
    private ResumeTurnEventPublisher turnEventPublisher;
    @Mock
    private QuestionSetRepository questionSetRepository;

    private InterviewRuntimeState state;
    private ResumeSkeleton skeleton;
    private InterviewPlan plan;

    @BeforeEach
    void setUp() {
        state = new InterviewRuntimeState("JUNIOR", null);
        skeleton = new ResumeSkeleton("r1", "h1", null, "backend", List.of(), Map.of());
        plan = createPlan();

        lenient().when(runtimeStateStore.get(anyLong())).thenReturn(state);
        lenient().doAnswer(inv -> {
            java.util.function.Consumer<InterviewRuntimeState> mutator = inv.getArgument(1);
            mutator.accept(state);
            return null;
        }).when(runtimeStateStore).update(anyLong(), any());
        lenient().when(questionSetRepository.findByInterviewIdAndCategory(anyLong(), eq(QuestionSetCategory.RESUME_BASED)))
                .thenReturn(java.util.Optional.empty());
        // 기본: policy 가 현재 mode 그대로 반환, hard timeout 없음
        lenient().when(modeTransitionPolicy.advanceToWrapUpIfDue(anyLong(), anyLong(), any()))
                .thenAnswer(inv -> inv.getArgument(2));
        lenient().when(modeTransitionPolicy.isHardTimeoutExceeded(anyInt(), anyLong())).thenReturn(false);
    }

    @Nested
    @DisplayName("기본 모드 라우팅")
    class DefaultModeRouting {

        @Test
        @DisplayName("정상 답변이면 현재 mode 핸들러로 라우팅된다")
        void processUserTurn_answer_routesToModeHandler() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false, 11L));

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("Q");
            then(playgroundHandler).should().handle(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("WRAP_UP 자동 전이 (정책 위임)")
    class WrapUpTransition {

        @Test
        @DisplayName("policy 가 WRAP_UP 으로 전이를 알리면 wrapUpHandler 가 호출된다")
        void processUserTurn_policyAdvancesToWrapUp_routesToWrapUpHandler() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(1L);
            given(modeTransitionPolicy.advanceToWrapUpIfDue(anyLong(), eq(1L), any()))
                    .willAnswer(inv -> {
                        state.transitionTo(ResumeMode.WRAP_UP);
                        return ResumeMode.WRAP_UP;
                    });
            given(wrapUpHandler.handle(any(), any(), any(), any(), any(), anyLong(), anyBoolean(), any()))
                    .willReturn(new WrapUpModeHandler.WrapUpTurnResult(
                            FollowUpResponse.builder().question("마무리").presentToUser(true).build(), 12L));

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("마무리");
            then(wrapUpHandler).should().handle(any(), any(), any(), any(), any(), anyLong(), anyBoolean(), any());
        }

        @Test
        @DisplayName("hard timeout 초과 시 RESUME_HARD_TIMEOUT 응답이 반환된다")
        void processUserTurn_hardTimeoutExceeded_returnsHardTimeoutResponse() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(0L);
            given(modeTransitionPolicy.advanceToWrapUpIfDue(anyLong(), anyLong(), any()))
                    .willReturn(ResumeMode.WRAP_UP);
            given(modeTransitionPolicy.isHardTimeoutExceeded(eq(30), eq(0L))).willReturn(true);

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getType()).isEqualTo("RESUME_HARD_TIMEOUT");
            assertThat(response.isFollowUpExhausted()).isTrue();
            then(wrapUpHandler).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("모드별 라우팅")
    class ModeRouting {

        @Test
        @DisplayName("mode=INTERROGATION 이면 interrogationHandler 로 라우팅된다")
        void processUserTurn_interrogationMode_routesToInterrogationHandler() {
            state.transitionTo(ResumeMode.INTERROGATION);
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(interrogationHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new InterrogationTurnResult(
                            FollowUpResponse.builder().question("L2 질문").presentToUser(true).build(), 13L));

            FollowUpResponse response = orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("L2 질문");
            then(interrogationHandler).should().handle(any(), any(), any(), any(), any(), any());
            then(playgroundHandler).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("세션 시작")
    class SessionStart {

        @Test
        @DisplayName("RESUME_OPENER 가 없으면 playgroundHandler.handleOpener 를 호출해 opener 를 생성한다")
        void startSession_noExistingOpener_callsHandleOpener() {
            given(playgroundHandler.handleOpener(anyLong(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.OpenerResult(
                            FollowUpResponse.builder()
                                    .question("Redis 프로젝트를 소개해주세요")
                                    .presentToUser(true)
                                    .type("RESUME_OPENER")
                                    .build(), null));

            FollowUpResponse response = orchestrator.startSession(1L, 30, skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("Redis 프로젝트를 소개해주세요");
            assertThat(response.isPresentToUser()).isTrue();
            then(playgroundHandler).should().handleOpener(eq(1L), any(), eq(skeleton), eq(plan));
        }

        @Test
        @DisplayName("startSession 은 clockWatcher.markStart 를 호출하지 않는다")
        void startSession_doesNotCallMarkStart() {
            given(playgroundHandler.handleOpener(anyLong(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.OpenerResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), null));

            orchestrator.startSession(1L, 30, skeleton, plan);

            then(clockWatcher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 RESUME_OPENER 가 있으면 재생성 없이 기존 텍스트로 응답을 반환한다")
        void startSession_existingOpener_reusesWithoutCallingHandleOpener() {
            com.rehearse.api.domain.question.entity.QuestionSet qs =
                    com.rehearse.api.domain.question.entity.QuestionSet.builder()
                            .category(QuestionSetCategory.RESUME_BASED)
                            .orderIndex(0)
                            .build();
            com.rehearse.api.domain.question.entity.Question existingOpener =
                    com.rehearse.api.domain.question.entity.Question.resume(
                            qs,
                            com.rehearse.api.domain.question.entity.QuestionType.RESUME_OPENER,
                            "기존 opener 질문입니다",
                            null,
                            null,
                            0);
            qs.addQuestion(existingOpener);
            given(questionSetRepository.findByInterviewIdAndCategory(eq(1L), eq(QuestionSetCategory.RESUME_BASED)))
                    .willReturn(java.util.Optional.of(qs));

            FollowUpResponse response = orchestrator.startSession(1L, 30, skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("기존 opener 질문입니다");
            assertThat(response.isPresentToUser()).isTrue();
            then(playgroundHandler).shouldHaveNoInteractions();
            then(clockWatcher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("processUserTurn 첫 호출 시 clockWatcher.markStart 가 호출된다")
        void processUserTurn_firstCall_marksStart() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false, 14L));

            orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            then(clockWatcher).should().markStart(1L);
        }
    }

    @Nested
    @DisplayName("TurnCompletedEvent 발행")
    class EventPublishing {

        @Test
        @DisplayName("processUserTurn 후 turnEventPublisher.publish 가 현재 mode 와 함께 호출된다")
        void processUserTurn_publishes_event() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("사용자답변텍스트", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false, 42L));

            orchestrator.processUserTurn(1L, 30, "질문텍스트", "사용자답변텍스트", List.of(), skeleton, plan);

            ArgumentCaptor<ResumeMode> modeCaptor = ArgumentCaptor.forClass(ResumeMode.class);
            ArgumentCaptor<String> answerCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> qIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(turnEventPublisher).should().publish(eq(1L), anyLong(), any(),
                    modeCaptor.capture(), anyInt(), eq(skeleton), answerCaptor.capture(), qIdCaptor.capture());

            assertThat(modeCaptor.getValue()).isEqualTo(ResumeMode.PLAYGROUND);
            assertThat(answerCaptor.getValue()).isEqualTo("사용자답변텍스트");
            assertThat(qIdCaptor.getValue()).isEqualTo(42L);
        }

        @Test
        @DisplayName("표시할 질문이 있는데 questionId 가 없으면 결함으로 예외를 던진다")
        void processUserTurn_presentedQuestionWithoutQuestionId_throwsException() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("사용자답변텍스트", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false, null));

            assertThatThrownBy(() -> orchestrator.processUserTurn(
                    1L, 30, "질문텍스트", "사용자답변텍스트", List.of(), skeleton, plan))
                    .isInstanceOf(BusinessException.class);

            then(turnEventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("표시할 질문 없이 종료된 턴은 TurnCompletedEvent 를 발행하지 않는다")
        void processUserTurn_exhaustedWithoutQuestion_doesNotPublish() {
            state.transitionTo(ResumeMode.INTERROGATION);
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("사용자답변텍스트", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(interrogationHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new InterrogationTurnResult(
                            FollowUpResponse.builder()
                                    .skip(true)
                                    .presentToUser(false)
                                    .followUpExhausted(true)
                                    .type("RESUME_INTERROGATION_EXHAUSTED")
                                    .build(), null));

            FollowUpResponse response = orchestrator.processUserTurn(
                    1L, 30, "질문텍스트", "사용자답변텍스트", List.of(), skeleton, plan);

            assertThat(response.isFollowUpExhausted()).isTrue();
            then(turnEventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("컨텍스트 토큰 예산 초과 graceful 종료 (P1-2)")
    class ContextBudgetExceeded {

        @Test
        @DisplayName("핸들러가 CONTEXT_BUDGET_EXCEEDED 를 던지면 200 응답 + type=CONTEXT_BUDGET_EXCEEDED 로 변환된다")
        void processUserTurn_contextBudgetExceeded_returnsGracefulResponse() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willThrow(new BusinessException(AiErrorCode.CONTEXT_BUDGET_EXCEEDED));

            FollowUpResponse response = orchestrator.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan);

            assertThat(response.getType()).isEqualTo("CONTEXT_BUDGET_EXCEEDED");
            assertThat(response.isFollowUpExhausted()).isTrue();
            assertThat(response.isSkip()).isTrue();
            assertThat(response.isPresentToUser()).isFalse();
            then(turnEventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("CONTEXT_BUDGET_EXCEEDED 가 아닌 BusinessException 은 그대로 재throw 된다")
        void processUserTurn_otherBusinessException_rethrows() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willThrow(new BusinessException(AiErrorCode.RESPONSE_INVALID));

            assertThatThrownBy(() -> orchestrator.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(AiErrorCode.RESPONSE_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("[진행차단진단] WARN 로그")
    class BlockingDiagnosticsWarnLog {

        private ListAppender<ILoggingEvent> appender;
        private Logger targetLogger;

        @BeforeEach
        void attachAppender() {
            targetLogger = (Logger) LoggerFactory.getLogger(ResumeInterviewOrchestrator.class);
            appender = new ListAppender<>();
            appender.start();
            targetLogger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            targetLogger.detachAppender(appender);
        }

        @Test
        @DisplayName("publish-skip 분기 진입 시 track=RESUME WARN 로그를 4개 키와 함께 기록")
        void publishSkip_logsBlockingDiagnosticsWarn() {
            state.transitionTo(ResumeMode.INTERROGATION);
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(interrogationHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new InterrogationTurnResult(
                            FollowUpResponse.builder()
                                    .skip(true)
                                    .presentToUser(false)
                                    .followUpExhausted(true)
                                    .type("RESUME_INTERROGATION_EXHAUSTED")
                                    .build(), null));

            orchestrator.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan);

            ILoggingEvent warn = findWarn(appender);
            assertThat(warn.getLevel()).isEqualTo(Level.WARN);
            String formatted = warn.getFormattedMessage();
            assertThat(formatted).contains("[진행차단진단]");
            assertThat(formatted).contains("interviewId=1");
            assertThat(formatted).contains("track=RESUME");
            assertThat(formatted).contains("stage=interrogation");
            assertThat(formatted).contains("reason=publish-skip");
        }

        private ILoggingEvent findWarn(ListAppender<ILoggingEvent> appender) {
            return appender.list.stream()
                    .filter(e -> e.getLevel() == Level.WARN)
                    .filter(e -> e.getFormattedMessage().contains("[진행차단진단]"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("[진행차단진단] WARN 로그 미발견"));
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
        return new InterviewPlan("plan-001", List.of(projectPlan));
    }
}
