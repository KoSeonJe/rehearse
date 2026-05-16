package com.rehearse.api.domain.resume.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.ChainStep;
import com.rehearse.api.domain.resume.entity.InterrogationChain;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.StepType;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.ResilientAiClient;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;

@DisplayName("ResumeInterviewOrchestrator Service Integration - WRAP_UP 제거 후 종료 분기 통합 검증")
class ResumeInterviewOrchestratorIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private ResumeInterviewOrchestrator orchestrator;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRuntimeStateCache runtimeStateCache;

    @Autowired
    private QuestionScoreRepository questionScoreRepository;

    @Autowired
    private ResumeModeTransitionPolicy modeTransitionPolicy;

    @Autowired
    private ClockWatcher clockWatcher;

    @MockitoBean
    private ResilientAiClient resilientAiClient;

    @BeforeEach
    void resetMockAndPolicy() {
        reset(resilientAiClient);
        ReflectionTestUtils.setField(modeTransitionPolicy, "hardTimeoutMin", 10L);
        ReflectionTestUtils.setField(modeTransitionPolicy, "playgroundMaxTurns", 3);
        ReflectionTestUtils.setField(clockWatcher, "clock", Clock.systemDefaultZone());
    }

    @Test
    @DisplayName("정상 다중 턴: PLAYGROUND -> INTERROGATION 전이 후 question 적재는 모드별 type 만 사용 (RESUME_WRAP_UP INSERT 0건)")
    void normalMultiTurn_persistsOnlyPlaygroundOrInterrogationType() {
        Long interviewId = persistInterviewAndInitState();

        given(resilientAiClient.chat(any())).willAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            return chatResponseFor(request.callType(), true);
        });

        FollowUpResponse first = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "Redis 기반 캐시 시스템을 구축했습니다.",
                List.of(), createSkeleton(), createPlan(), false);

        assertThat(first.getQuestion()).isNotBlank();

        long wrapUpCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE question_type = 'RESUME_WRAP_UP'", Long.class);
        assertThat(wrapUpCount).isZero();

        List<String> types = jdbcTemplate.queryForList(
                "SELECT question_type FROM question WHERE question_set_id IN " +
                        "(SELECT id FROM question_set WHERE interview_id = ?) ORDER BY id",
                String.class, interviewId);
        assertThat(types).isNotEmpty();
        assertThat(types).allSatisfy(t -> assertThat(t).isIn("RESUME_PLAYGROUND", "RESUME_INTERROGATION"));

        InterviewRuntimeState state = runtimeStateCache.get(interviewId);
        assertThat(state.getResumeMode()).isEqualTo(ResumeMode.INTERROGATION);
    }

    @Test
    @DisplayName("terminate=true: 답변 분석은 수행하되 question INSERT 0 + FollowUpQuestionCreatedEvent 미발행 (question_score 적재 0)")
    void terminateTrue_skipsInsertAndPublish() {
        Long interviewId = persistInterviewAndInitState();

        given(resilientAiClient.chat(any())).willAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            return chatResponseFor(request.callType(), false);
        });

        FollowUpResponse response = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "마무리하겠습니다.",
                List.of(), createSkeleton(), createPlan(), true);

        assertThat(response.isFollowUpExhausted()).isTrue();
        assertThat(response.isSkip()).isTrue();
        assertThat(response.isPresentToUser()).isFalse();
        assertThat(response.getType()).isNull();

        long questionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE question_set_id IN " +
                        "(SELECT id FROM question_set WHERE interview_id = ?)", Long.class, interviewId);
        assertThat(questionCount).isZero();

        assertNoScoresDuring(interviewId, Duration.ofMillis(500));
    }

    @Test
    @DisplayName("hard timeout backstop (terminate=false + duration 초과): RESUME_HARD_TIMEOUT 응답 + question INSERT 0")
    void hardTimeoutBackstop_returnsHardTimeoutResponse() {
        Long interviewId = persistInterviewAndInitState();
        installFastForwardClock(interviewId, 100);
        ReflectionTestUtils.setField(modeTransitionPolicy, "hardTimeoutMin", 0L);

        given(resilientAiClient.chat(any())).willAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            return chatResponseFor(request.callType(), false);
        });

        FollowUpResponse response = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "답변 텍스트입니다.",
                List.of(), createSkeleton(), createPlan(), false);

        assertThat(response.getType()).isEqualTo("RESUME_HARD_TIMEOUT");
        assertThat(response.isFollowUpExhausted()).isTrue();
        assertThat(response.isSkip()).isTrue();
        assertThat(response.isPresentToUser()).isFalse();

        long questionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE question_set_id IN " +
                        "(SELECT id FROM question_set WHERE interview_id = ?)", Long.class, interviewId);
        assertThat(questionCount).isZero();
    }

    @Test
    @DisplayName("terminate=true 와 hard timeout 동시 발생 시 hard timeout 우선 (RESUME_HARD_TIMEOUT)")
    void terminateAndHardTimeoutBoth_hardTimeoutWins() {
        Long interviewId = persistInterviewAndInitState();
        installFastForwardClock(interviewId, 100);
        ReflectionTestUtils.setField(modeTransitionPolicy, "hardTimeoutMin", 0L);

        given(resilientAiClient.chat(any())).willAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            return chatResponseFor(request.callType(), false);
        });

        FollowUpResponse response = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "답변 텍스트입니다.",
                List.of(), createSkeleton(), createPlan(), true);

        assertThat(response.getType()).isEqualTo("RESUME_HARD_TIMEOUT");
    }

    @Test
    @DisplayName("첫 턴 terminate (Playground opener 답변 케이스): 분석은 수행 + question INSERT 0 + 예외 미발생")
    void firstTurnTerminate_runsAnalysisWithoutInsertOrException() {
        Long interviewId = persistInterviewAndInitState();

        given(resilientAiClient.chat(any())).willAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            return chatResponseFor(request.callType(), false);
        });

        FollowUpResponse response = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "오프너 답변입니다. 마무리할게요.",
                List.of(), createSkeleton(), createPlan(), true);

        assertThat(response.isFollowUpExhausted()).isTrue();
        assertThat(response.isSkip()).isTrue();
        assertThat(response.isPresentToUser()).isFalse();

        long questionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE question_set_id IN " +
                        "(SELECT id FROM question_set WHERE interview_id = ?)", Long.class, interviewId);
        assertThat(questionCount).isZero();

        InterviewRuntimeState state = runtimeStateCache.get(interviewId);
        assertThat(state.getTurnAnalysisCache()).isNotEmpty();
    }

    @Test
    @DisplayName("RESUME PLAYGROUND turn 응답 DTO questionId 가 자기 질문 ID 를 보유 + mismatch WARN 미발생 (Issue #433 회귀)")
    void playgroundTurn_responseCarriesSelfQuestionIdWithoutMismatchWarn() {
        Long interviewId = persistInterviewAndInitState();

        Logger orchestratorLogger = (Logger) LoggerFactory.getLogger(ResumeInterviewOrchestrator.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        orchestratorLogger.addAppender(logAppender);
        try {
            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), false);
            });

            FollowUpResponse response = orchestrator.processUserTurn(
                    interviewId, 30, "프로젝트를 소개해주세요.", "Redis 기반 캐시 시스템을 구축했습니다.",
                    List.of(), createSkeleton(), createPlan(), false);

            assertThat(response.getQuestionId())
                    .as("응답 DTO 가 자기 질문 ID 를 보유해야 FE 매핑 정상")
                    .isNotNull();
            assertThat(response.getType())
                    .as("RESUME 트랙 type 접두어 유지 — PLAYGROUND/INTERROGATION 분기 모두 허용")
                    .startsWith("RESUME_");
            assertThat(response.getQuestion()).isNotBlank();

            assertThat(logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList())
                    .as("정상 흐름 — mismatch WARN 미발생")
                    .noneMatch(m -> m.contains("response-questionid-mismatch"));
        } finally {
            orchestratorLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    @Test
    @DisplayName("RESUME INTERROGATION turn 응답 DTO questionId 보유 + type RESUME_INTERROGATION_* + WARN 미발생 (Issue #433 회귀)")
    void interrogationTurn_responseCarriesQuestionIdAndInterrogationType() {
        Long interviewId = persistInterviewAndInitState();
        runtimeStateCache.update(interviewId, s -> s.transitionTo(ResumeMode.INTERROGATION));

        Logger orchestratorLogger = (Logger) LoggerFactory.getLogger(ResumeInterviewOrchestrator.class);
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
        logAppender.start();
        orchestratorLogger.addAppender(logAppender);
        try {
            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), false);
            });

            FollowUpResponse response = orchestrator.processUserTurn(
                    interviewId, 30, "Redis 의 데이터 구조 선택 근거를 설명해주세요", "기본은 String, 정렬이 필요하면 ZSET 을 사용했습니다.",
                    List.of(), createSkeleton(), createPlan(), false);

            assertThat(response.getQuestionId())
                    .as("INTERROGATION turn 응답 DTO 가 자기 질문 ID 를 보유해야 FE 매핑 정상")
                    .isNotNull();
            assertThat(response.getType())
                    .as("INTERROGATION turn 응답 type 은 RESUME_INTERROGATION 접두어 유지")
                    .startsWith("RESUME_INTERROGATION");

            assertThat(logAppender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList())
                    .as("정상 흐름 — questionId 누락/불일치 WARN 미발생")
                    .noneMatch(m -> m.contains("response-questionid-missing")
                            || m.contains("response-questionid-mismatch"));
        } finally {
            orchestratorLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    @Test
    @DisplayName("멱등성: terminate=true 후 동일 interviewId 재요청 시 예외 없이 종료 응답 echo + 추가 INSERT 0")
    void idempotentTerminate_repeatedRequestsRemainSafe() {
        Long interviewId = persistInterviewAndInitState();

        given(resilientAiClient.chat(any())).willAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            return chatResponseFor(request.callType(), false);
        });

        FollowUpResponse first = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "마무리합니다.",
                List.of(), createSkeleton(), createPlan(), true);
        FollowUpResponse second = orchestrator.processUserTurn(
                interviewId, 30, "프로젝트를 소개해주세요.", "마무리합니다.",
                List.of(), createSkeleton(), createPlan(), true);

        assertThat(first.isFollowUpExhausted()).isTrue();
        assertThat(second.isFollowUpExhausted()).isTrue();

        long questionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE question_set_id IN " +
                        "(SELECT id FROM question_set WHERE interview_id = ?)", Long.class, interviewId);
        assertThat(questionCount).isZero();
    }

    @Nested
    @DisplayName("PLAYGROUND hard cap (#434)")
    class PlaygroundHardCap {

        @Test
        @DisplayName("누적 턴 수가 임계값에 도달하면 LLM 호출 없이 INTERROGATION 으로 강제 전환된다")
        void transitionsToInterrogation_when_playgroundTurnsReachHardCap() {
            Long interviewId = persistInterviewAndInitState();
            runtimeStateCache.update(interviewId, s -> s.getPlaygroundTurns().set(3));

            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), false);
            });

            FollowUpResponse response = orchestrator.processUserTurn(
                    interviewId, 30, "프로젝트를 소개해주세요.", "Redis 기반 캐시 시스템을 구축했습니다.",
                    List.of(), createSkeleton(), createPlan(), false);

            assertThat(response.getType()).startsWith("RESUME_INTERROGATION");
            assertThat(response.getQuestion()).isNotBlank();
            assertThat(response.getQuestionId()).isNotNull();

            InterviewRuntimeState state = runtimeStateCache.get(interviewId);
            assertThat(state.getResumeMode()).isEqualTo(ResumeMode.INTERROGATION);

            assertThat(callTypeCount("resume_playground_responder")).isZero();
            assertThat(callTypeCount("resume_chain_interrogator")).isEqualTo(1);
        }

        @Test
        @DisplayName("LLM 이 임계값 이전에 자가 전환 신호를 보내면 기존 흐름대로 INTERROGATION 으로 전이한다")
        void keepsLlmDrivenTransition_when_responderSignalsSwitchEarly() {
            Long interviewId = persistInterviewAndInitState();

            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), true);
            });

            FollowUpResponse response = orchestrator.processUserTurn(
                    interviewId, 30, "프로젝트를 소개해주세요.", "Redis 기반 캐시 시스템을 구축했습니다.",
                    List.of(), createSkeleton(), createPlan(), false);

            assertThat(response.getType()).startsWith("RESUME_INTERROGATION");
            InterviewRuntimeState state = runtimeStateCache.get(interviewId);
            assertThat(state.getResumeMode()).isEqualTo(ResumeMode.INTERROGATION);

            assertThat(callTypeCount("resume_playground_responder")).isEqualTo(1);
        }

        @Test
        @DisplayName("누적 턴 수가 임계값 미만이고 LLM 신호가 없으면 워밍업 단계가 유지된다")
        void staysInPlayground_when_belowCapAndNoSwitchSignal() {
            Long interviewId = persistInterviewAndInitState();

            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), false);
            });

            FollowUpResponse response = orchestrator.processUserTurn(
                    interviewId, 30, "프로젝트를 소개해주세요.", "Redis 기반 캐시 시스템을 구축했습니다.",
                    List.of(), createSkeleton(), createPlan(), false);

            assertThat(response.getType()).isEqualTo("RESUME_PLAYGROUND");
            assertThat(response.getQuestion()).isNotBlank();

            InterviewRuntimeState state = runtimeStateCache.get(interviewId);
            assertThat(state.getResumeMode()).isEqualTo(ResumeMode.PLAYGROUND);

            assertThat(callTypeCount("resume_playground_responder")).isEqualTo(1);
        }

        private long callTypeCount(String callType) {
            return mockingDetails(resilientAiClient).getInvocations().stream()
                    .filter(inv -> "chat".equals(inv.getMethod().getName()))
                    .map(inv -> (ChatRequest) inv.getArgument(0))
                    .filter(req -> req != null && callType.equals(req.callType()))
                    .count();
        }
    }

    @Nested
    @DisplayName("PLAYGROUND→INTERROGATION 전환 정합성 (#466)")
    class ModeTransitionContextIntegrity {

        @Test
        @DisplayName("hard cap 으로 모드 전환 시 직전 PLAYGROUND 답변 텍스트가 chain prompt USER_ANSWER 에 전달된다")
        void modeTransition_passesPreviousAnswerTextToChainPrompt() {
            Long interviewId = persistInterviewAndInitState();
            runtimeStateCache.update(interviewId, s -> s.getPlaygroundTurns().set(3));

            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), false);
            });

            String previousAnswer = "리허설 프로젝트에서 가장 기억에 남는 순간은 Redis 캐시 도입 결정이었습니다.";
            orchestrator.processUserTurn(
                    interviewId, 30, "프로젝트를 소개해주세요.", previousAnswer,
                    List.of(), createSkeleton(), createPlan(), false);

            String chainPromptBody = capturePromptBody("resume_chain_interrogator");
            assertThat(chainPromptBody)
                    .as("직전 PLAYGROUND 답변이 chain prompt USER_ANSWER 에 plumbed (silent drop 차단)")
                    .contains(previousAnswer);
        }

        @Test
        @DisplayName("hard cap 모드 전환 시 정상 analysis 의 ANSWER_QUALITY 가 chain prompt 에 plumbed (DEFAULT_ANSWER_QUALITY=2 가짜 평균값 차단)")
        void modeTransition_emptyAnalysisPropagatesQuality1() {
            Long interviewId = persistInterviewAndInitState();
            runtimeStateCache.update(interviewId, s -> s.getPlaygroundTurns().set(3));

            given(resilientAiClient.chat(any())).willAnswer(invocation -> {
                ChatRequest request = invocation.getArgument(0);
                return chatResponseFor(request.callType(), false);
            });

            orchestrator.processUserTurn(
                    interviewId, 30, "프로젝트를 소개해주세요.", "직전 답변",
                    List.of(), createSkeleton(), createPlan(), false);

            String chainPromptBody = capturePromptBody("resume_chain_interrogator");
            assertThat(chainPromptBody)
                    .as("ANSWER_QUALITY 영역이 mock analyzer 의 실제 반환값(3) 을 반영 — 가짜 평균값 2 가 아님")
                    .contains("ANSWER_QUALITY: 3")
                    .doesNotContain("ANSWER_QUALITY: 2");
        }

        private String capturePromptBody(String callType) {
            return mockingDetails(resilientAiClient).getInvocations().stream()
                    .filter(inv -> "chat".equals(inv.getMethod().getName()))
                    .map(inv -> (ChatRequest) inv.getArgument(0))
                    .filter(req -> req != null && callType.equals(req.callType()))
                    .findFirst()
                    .map(req -> String.join("\n",
                            req.messages().stream().map(m -> m.content()).toList()))
                    .orElseThrow(() -> new AssertionError("callType=" + callType + " 호출 미발생"));
        }
    }

    private Long persistInterviewAndInitState() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("orchestrator-int-" + System.nanoTime() + "@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-orch-" + System.nanoTime())
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);
        runtimeStateCache.evict(interview.getId());
        runtimeStateCache.getOrInit(interview.getId(),
                () -> InterviewRuntimeState.seed("JUNIOR", createSkeleton(), createPlan()));
        return interview.getId();
    }

    private void installFastForwardClock(Long interviewId, int forwardMinutes) {
        clockWatcher.markStart(interviewId);
        Instant baseStartedAt = runtimeStateCache.get(interviewId).getStartedAt();
        Instant fastForward = baseStartedAt.plus(Duration.ofMinutes(forwardMinutes));
        Clock fixedFuture = Clock.fixed(fastForward, ZoneId.systemDefault());
        ReflectionTestUtils.setField(clockWatcher, "clock", fixedFuture);
    }

    private void assertNoScoresDuring(Long interviewId, Duration duration) {
        long deadline = System.nanoTime() + duration.toNanos();
        do {
            assertThat(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId)).isEmpty();
            sleep(50);
        } while (System.nanoTime() < deadline);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("대기 중 interrupt 발생", e);
        }
    }

    private ResumeSkeleton createSkeleton() {
        ChainStep what = new ChainStep(1, StepType.WHAT, "Redis 란?");
        ChainStep how = new ChainStep(2, StepType.HOW, "어떻게 사용했나?");
        ChainStep whyMech = new ChainStep(3, StepType.WHY_MECH, "왜 Redis 인가?");
        ChainStep tradeoff = new ChainStep(4, StepType.TRADEOFF, "트레이드오프?");
        InterrogationChain chain = new InterrogationChain("Redis 캐싱", 0.9, List.of(what, how, whyMech, tradeoff));
        Project project = new Project("proj1", "Redis 캐싱 프로젝트",
                List.of(), "", "", List.of(), List.of(), List.of(chain));
        return new ResumeSkeleton("resume1", "hash", null, "backend", List.of(project), Map.of());
    }

    private InterviewPlan createPlan() {
        ChainReference ref = new ChainReference("proj1::redis", "Redis 캐싱", 1, List.of(1, 2, 3, 4));
        PlaygroundPhase pg = new PlaygroundPhase("프로젝트를 소개해주세요.", List.of("Redis 사용", "캐싱 전략"));
        InterrogationPhase ip = new InterrogationPhase(List.of(ref), List.of());
        ProjectPlan pp = new ProjectPlan("proj1", "Redis 캐싱 프로젝트", 1, pg, ip);
        return new InterviewPlan("plan-001", List.of(pp));
    }

    private ChatResponse chatResponseFor(String callType, boolean switchToInterrogation) {
        String json = switch (callType) {
            case "answer_analyzer" -> """
                    {
                      "turn_id": 1,
                      "claims": [],
                      "missing_perspectives": [],
                      "unstated_assumptions": [],
                      "answer_quality": 3,
                      "recommended_next_action": "DEEP_DIVE"
                    }
                    """;
            case "resume_playground_responder" -> """
                    {
                      "question": "%s",
                      "tts_question": "후속 질문",
                      "reason": "근거 추적",
                      "should_switch_to_interrogation": %s,
                      "switch_conditions_met": {"a_covered": %s, "b_length_ok": %s, "c_signal": %s, "d_turn_limit": %s},
                      "best_answer": "방금 답변 중 결정적인 선택을 짧게 풀어내고 결과까지 잇는 게 좋습니다."
                    }
                    """.formatted(
                    switchToInterrogation ? "Redis 데이터 구조 선택 근거를 설명해주세요" : "그 결정이 왜 Redis 였나요?",
                    switchToInterrogation, switchToInterrogation, switchToInterrogation, switchToInterrogation, switchToInterrogation);
            case "resume_chain_interrogator" -> """
                    {
                      "question": "Redis 의 데이터 구조 선택 근거를 설명해주세요",
                      "tts_question": "Redis 의 데이터 구조 선택 근거를 설명해주세요",
                      "reason": "L2 진입",
                      "next_action": "LEVEL_UP",
                      "next_level": 2,
                      "best_answer": "현재 단계에 맞춰 핵심 개념과 선택 근거를 한 문단으로 정리하세요."
                    }
                    """;
            case "rubric_scorer" -> """
                    {
                      "technical_depth": {"score": 2, "observation": "기술 깊이 확인", "evidence_quote": "답변"},
                      "reasoning_communication": {"score": 2, "observation": "설명 흐름 확인", "evidence_quote": "답변"},
                      "experience_concreteness": {"score": 2, "observation": "경험 구체성 확인", "evidence_quote": "답변"},
                      "factual_consistency": {"score": 2, "observation": "사실 일치 확인", "evidence_quote": "답변"},
                      "chain_depth": {"score": 2, "observation": "체인 깊이 확인", "evidence_quote": "답변"}
                    }
                    """;
            default -> "{}";
        };
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }
}
