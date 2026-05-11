package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.service.TurnAnalysisPipeline;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeInterviewService - FSM 라우팅 (PLAYGROUND/INTERROGATION 2단계)")
class ResumeInterviewServiceTest {

    @InjectMocks
    private ResumeInterviewService resumeInterviewService;

    @Mock
    private TurnAnalysisPipeline turnAnalysisPipeline;
    @Mock
    private PlaygroundModeHandler playgroundHandler;
    @Mock
    private InterrogationModeHandler interrogationHandler;
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
        lenient().when(questionSetRepository.findByInterviewIdAndCategory(anyLong(), eq(InterviewType.RESUME_BASED)))
                .thenReturn(java.util.Optional.empty());
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

            FollowUpResponse response = resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            assertThat(response.getQuestion()).isEqualTo("Q");
            then(playgroundHandler).should().handle(any(), any(), any(), any(), any(), any(), any());
        }

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

            FollowUpResponse response = resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

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

            FollowUpResponse response = resumeInterviewService.startSession(1L, 30, skeleton, plan);

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

            resumeInterviewService.startSession(1L, 30, skeleton, plan);

            then(clockWatcher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 RESUME_OPENER 가 있으면 재생성 없이 기존 텍스트로 응답을 반환한다 (응답 DTO 가 기존 OPENER 질문 ID 보유 — Issue #433 회귀)")
        void startSession_existingOpener_reusesWithoutCallingHandleOpener() {
            com.rehearse.api.domain.question.entity.QuestionSet qs =
                    com.rehearse.api.domain.question.entity.QuestionSet.builder()
                            .category(InterviewType.RESUME_BASED)
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
            org.springframework.test.util.ReflectionTestUtils.setField(existingOpener, "id", 7777L);
            qs.addQuestion(existingOpener);
            given(questionSetRepository.findByInterviewIdAndCategory(eq(1L), eq(InterviewType.RESUME_BASED)))
                    .willReturn(java.util.Optional.of(qs));

            FollowUpResponse response = resumeInterviewService.startSession(1L, 30, skeleton, plan);

            assertThat(response.getQuestion()).isEqualTo("기존 opener 질문입니다");
            assertThat(response.isPresentToUser()).isTrue();
            assertThat(response.getQuestionId())
                    .as("재사용 응답 DTO 가 기존 OPENER 질문 ID 를 보유해야 FE 매핑 정상")
                    .isEqualTo(7777L);
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

            resumeInterviewService.processUserTurn(1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

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

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문텍스트", "사용자답변텍스트", List.of(), skeleton, plan, false);

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
        @DisplayName("PLAYGROUND→INTERROGATION 전환 turn 의 publish mode 는 INTERROGATION 으로 전달된다 (Issue #472)")
        void processUserTurn_modeSwitch_publishesEffectiveModeAsInterrogation() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("PG 응답").presentToUser(true).build(),
                            true, 51L));
            given(interrogationHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new InterrogationTurnResult(
                            FollowUpResponse.builder().question("INT 첫 질문").presentToUser(true).build(), 71L));

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            ArgumentCaptor<ResumeMode> modeCaptor = ArgumentCaptor.forClass(ResumeMode.class);
            ArgumentCaptor<Long> qIdCaptor = ArgumentCaptor.forClass(Long.class);
            then(turnEventPublisher).should().publish(eq(1L), anyLong(), any(),
                    modeCaptor.capture(), anyInt(), eq(skeleton), any(), qIdCaptor.capture());

            assertThat(modeCaptor.getValue()).isEqualTo(ResumeMode.INTERROGATION);
            assertThat(qIdCaptor.getValue()).isEqualTo(71L);
        }

        @Test
        @DisplayName("PLAYGROUND→PLAYGROUND 연속 turn 의 publish mode 는 PLAYGROUND 로 유지된다")
        void processUserTurn_playgroundOnly_publishesPlayground() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("PG 다음").presentToUser(true).build(),
                            false, 52L));

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            ArgumentCaptor<ResumeMode> modeCaptor = ArgumentCaptor.forClass(ResumeMode.class);
            then(turnEventPublisher).should().publish(eq(1L), anyLong(), any(),
                    modeCaptor.capture(), anyInt(), eq(skeleton), any(), eq(52L));

            assertThat(modeCaptor.getValue()).isEqualTo(ResumeMode.PLAYGROUND);
            then(interrogationHandler).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("INTERROGATION→INTERROGATION 연속 turn 의 publish mode 는 INTERROGATION 으로 유지된다")
        void processUserTurn_interrogationOnly_publishesInterrogation() {
            state.transitionTo(ResumeMode.INTERROGATION);
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(interrogationHandler.handle(any(), any(), any(), any(), any(), any()))
                    .willReturn(new InterrogationTurnResult(
                            FollowUpResponse.builder().question("L2 질문").presentToUser(true).build(), 81L));

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            ArgumentCaptor<ResumeMode> modeCaptor = ArgumentCaptor.forClass(ResumeMode.class);
            then(turnEventPublisher).should().publish(eq(1L), anyLong(), any(),
                    modeCaptor.capture(), anyInt(), eq(skeleton), any(), eq(81L));

            assertThat(modeCaptor.getValue()).isEqualTo(ResumeMode.INTERROGATION);
            then(playgroundHandler).shouldHaveNoInteractions();
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

            assertThatThrownBy(() -> resumeInterviewService.processUserTurn(
                    1L, 30, "질문텍스트", "사용자답변텍스트", List.of(), skeleton, plan, false))
                    .isInstanceOf(BusinessException.class);

            then(turnEventPublisher).shouldHaveNoInteractions();
        }

    }

    @Nested
    @DisplayName("컨텍스트 토큰 예산 초과 graceful 종료")
    class ContextBudgetExceeded {

        @Test
        @DisplayName("핸들러가 CONTEXT_BUDGET_EXCEEDED 를 던지면 200 응답 + type=CONTEXT_BUDGET_EXCEEDED 로 변환된다")
        void processUserTurn_contextBudgetExceeded_returnsGracefulResponse() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willThrow(new BusinessException(AiErrorCode.CONTEXT_BUDGET_EXCEEDED));

            FollowUpResponse response = resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

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

            assertThatThrownBy(() -> resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(AiErrorCode.RESPONSE_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("응답 questionId 정합 가드 (Issue #433 회귀)")
    class ResponseQuestionIdGuard {

        private ListAppender<ILoggingEvent> logAppender;
        private Logger resumeInterviewServiceLogger;

        @BeforeEach
        void attachLogAppender() {
            resumeInterviewServiceLogger = (Logger) LoggerFactory.getLogger(ResumeInterviewService.class);
            logAppender = new ListAppender<>();
            logAppender.start();
            resumeInterviewServiceLogger.addAppender(logAppender);
        }

        @AfterEach
        void detachLogAppender() {
            resumeInterviewServiceLogger.detachAppender(logAppender);
            logAppender.stop();
        }

        @Test
        @DisplayName("응답 DTO questionId 가 null 이면 missing WARN 로그를 남기고 publish 는 진행한다")
        void processUserTurn_responseQuestionIdMissing_logsWarnAndPublishes() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            // handler 가 응답 DTO 에 questionId 미주입한 결함 흐름 강제 시뮬
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().question("Q").presentToUser(true).build(), false, 99L));

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            assertThat(warnMessages())
                    .as("missing WARN 발생")
                    .anyMatch(m -> m.contains("response-questionid-missing")
                            && m.contains("handlerQuestionId=99"));
            assertThat(warnMessages())
                    .as("missing 케이스에서는 mismatch WARN 미발생")
                    .noneMatch(m -> m.contains("response-questionid-mismatch"));
            then(turnEventPublisher).should().publish(eq(1L), anyLong(), any(),
                    any(), anyInt(), eq(skeleton), any(), eq(99L));
        }

        @Test
        @DisplayName("응답 DTO questionId 가 handler 와 다르면 mismatch WARN 로그를 남기고 publish 는 진행한다")
        void processUserTurn_responseQuestionIdMismatch_logsWarnAndPublishes() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().questionId(7L).question("Q").presentToUser(true).build(),
                            false, 99L));

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            assertThat(warnMessages())
                    .as("mismatch WARN 발생")
                    .anyMatch(m -> m.contains("response-questionid-mismatch")
                            && m.contains("handlerQuestionId=99")
                            && m.contains("responseQuestionId=7"));
            assertThat(warnMessages())
                    .as("mismatch 케이스에서는 missing WARN 미발생")
                    .noneMatch(m -> m.contains("response-questionid-missing"));
            then(turnEventPublisher).should().publish(eq(1L), anyLong(), any(),
                    any(), anyInt(), eq(skeleton), any(), eq(99L));
        }

        @Test
        @DisplayName("정상 흐름에서 response.questionId == handler.questionId 이면 missing/mismatch WARN 미발생")
        void processUserTurn_responseQuestionIdMatches_doesNotLogWarn() {
            given(turnAnalysisPipeline.analyze(any(), anyLong(), any(), any(), any()))
                    .willReturn(new TurnAnalysisResult("답변", createAnalysis()));
            given(clockWatcher.remainingMinutes(anyLong(), anyInt())).willReturn(10L);
            given(playgroundHandler.handle(any(), any(), any(), any(), any(), any(), any()))
                    .willReturn(new PlaygroundModeHandler.PlaygroundTurnResult(
                            FollowUpResponse.builder().questionId(42L).question("Q").presentToUser(true).build(),
                            false, 42L));

            resumeInterviewService.processUserTurn(
                    1L, 30, "질문", "답변", List.of(), skeleton, plan, false);

            assertThat(warnMessages())
                    .as("정상 매칭 케이스 — questionId WARN 미발생")
                    .noneMatch(m -> m.contains("response-questionid-mismatch")
                            || m.contains("response-questionid-missing"));
        }

        private List<String> warnMessages() {
            return logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
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
