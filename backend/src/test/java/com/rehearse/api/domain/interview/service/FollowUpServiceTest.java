package com.rehearse.api.domain.interview.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.domain.interview.entity.EvidenceStrength;
import com.rehearse.api.domain.interview.entity.AnswerFeedbackPerspective;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.service.InterviewPlanRuntimeCache;
import com.rehearse.api.domain.resume.service.ResumeSkeletonRuntimeCache;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.InterviewPlanPersister;
import com.rehearse.api.domain.resume.service.ResumeInterviewPlanner;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpService - 꼬리질문 생성 (AudioTurnAnalyzer + Step B v3)")
class FollowUpServiceTest {

    private FollowUpService followUpService;

    @Mock
    private AudioTurnAnalyzer audioTurnAnalyzer;

    @Mock
    private FollowUpQuestionWriter followUpQuestionWriter;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Mock
    private InterviewRuntimeStateCache runtimeStateStore;

    @Mock
    private AiCallMetrics aiCallMetrics;

    @Mock
    private ResumeInterviewOrchestrator resumeOrchestrator;

    @Mock
    private ResumeSkeletonPersister resumeSkeletonStore;

    @Mock
    private InterviewPlanPersister interviewPlanStore;

    @Mock
    private ResumeSkeletonRuntimeCache resumeSkeletonCache;

    @Mock
    private InterviewPlanRuntimeCache interviewPlanCache;

    @Mock
    private ResumeInterviewPlanner resumeInterviewPlanner;

    @Mock
    private InterviewFinder interviewFinder;

    @BeforeEach
    void setUp() {
        followUpService = new FollowUpService(
                audioTurnAnalyzer, followUpQuestionWriter,
                followUpTransactionHandler, runtimeStateStore, aiCallMetrics,
                resumeOrchestrator, resumeSkeletonStore, interviewPlanStore,
                resumeSkeletonCache, interviewPlanCache, resumeInterviewPlanner, interviewFinder);

        lenient().when(runtimeStateStore.getOrInit(any(), any()))
                .thenReturn(new InterviewRuntimeState("JUNIOR", null));

        // CS 트랙 기본 stub: skeleton=null 경로에서 interviewFinder 호출 시 CS interview 반환
        Interview csDefault = mock(Interview.class);
        lenient().when(csDefault.getInterviewTypes()).thenReturn(Set.of(InterviewType.CS_FUNDAMENTAL));
        lenient().when(csDefault.getUserId()).thenReturn(1L);
        lenient().when(csDefault.getLevel()).thenReturn(InterviewLevel.JUNIOR);
        lenient().when(interviewFinder.findById(any())).thenReturn(csDefault);
    }

    private static FollowUpContext context(int nextOrderIndex, int maxFollowUpRounds) {
        return new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, nextOrderIndex,
                com.rehearse.api.domain.question.entity.ReferenceType.MODEL_ANSWER, maxFollowUpRounds);
    }

    private static AnswerAnalysis analysisOf(RecommendedNextAction action) {
        return new AnswerAnalysis(
                50L,
                List.of(new Claim("핵심 주장", 3, EvidenceStrength.WEAK, "topic")),
                List.of(AnswerFeedbackPerspective.RELIABILITY),
                List.of("가정"),
                3,
                action);
    }

    private static TurnAnalysisResult turn(String answerText, AnswerAnalysis analysis) {
        return new TurnAnalysisResult(answerText, analysis);
    }

    private static GeneratedFollowUp stepBQuestion(String question) {
        GeneratedFollowUp f = new GeneratedFollowUp();
        ReflectionTestUtils.setField(f, "question", question);
        ReflectionTestUtils.setField(f, "ttsQuestion", question);
        ReflectionTestUtils.setField(f, "reason", "r");
        ReflectionTestUtils.setField(f, "type", "DEEP_DIVE");
        ReflectionTestUtils.setField(f, "modelAnswer", "m");
        ReflectionTestUtils.setField(f, "answerText", "x");
        ReflectionTestUtils.setField(f, "selectedPerspective", "RELIABILITY");
        ReflectionTestUtils.setField(f, "skip", Boolean.FALSE);
        return f;
    }

    private static GeneratedFollowUp stepBSkip(String reason) {
        GeneratedFollowUp f = new GeneratedFollowUp();
        ReflectionTestUtils.setField(f, "skip", Boolean.TRUE);
        ReflectionTestUtils.setField(f, "skipReason", reason);
        return f;
    }

    private static GeneratedFollowUp stepBBlankQuestion() {
        GeneratedFollowUp f = new GeneratedFollowUp();
        ReflectionTestUtils.setField(f, "question", "  ");
        ReflectionTestUtils.setField(f, "skip", Boolean.FALSE);
        ReflectionTestUtils.setField(f, "type", "DEEP_DIVE");
        return f;
    }

    private static MockMultipartFile audio() {
        return new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private static FollowUpRequest request(String questionContent) {
        FollowUpRequest r = new FollowUpRequest();
        ReflectionTestUtils.setField(r, "questionSetId", 10L);
        ReflectionTestUtils.setField(r, "questionContent", questionContent);
        return r;
    }

    @Nested
    @DisplayName("generateFollowUp 메서드")
    class GenerateFollowUp {

        @Test
        @DisplayName("ANSWER 경로 — Step B 결과로 응답 빌드")
        void generateFollowUp_answerPath_buildsFromStepB() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(eq(1L), eq(50L), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("HashMap 은 해시 기반입니다.",
                            analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(FollowUpGenerationRequest.class), any(AnswerAnalysis.class), any(AskedPerspectives.class)))
                    .willReturn(stepBQuestion("Step B 가 만든 꼬리질문"));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.TECH_FOLLOWUP).questionText("Step B 가 만든 꼬리질문").orderIndex(1).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 100L);
            given(followUpTransactionHandler.saveFollowUpResultAndPublishEvent(eq(1L), any(FollowUpContext.class), any(GeneratedFollowUp.class), any(TurnAnalysisResult.class)))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 1));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("HashMap 충돌 해결?"), audio());

            assertThat(response.getQuestionId()).isEqualTo(100L);
            assertThat(response.getQuestion()).isEqualTo("Step B 가 만든 꼬리질문");
            assertThat(response.getType()).isEqualTo("DEEP_DIVE");
            assertThat(response.getSelectedPerspective()).isEqualTo("RELIABILITY");
            assertThat(response.isFollowUpExhausted()).isFalse();
            then(followUpQuestionWriter).should().write(any(), any(), any());
        }

        @Test
        @DisplayName("ANSWER 경로 — 누적 카운트가 maxFollowUpRounds 도달 시 followUpExhausted=true")
        void generateFollowUp_atMaxRounds_returnsExhaustedTrue() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(2, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("답변", analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(), any(), any())).willReturn(stepBQuestion("Q2"));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.TECH_FOLLOWUP).questionText("Q2").orderIndex(2).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 200L);
            given(followUpTransactionHandler.saveFollowUpResultAndPublishEvent(eq(1L), any(FollowUpContext.class), any(GeneratedFollowUp.class), any(TurnAnalysisResult.class)))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 2));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.isSkip()).isFalse();
            assertThat(response.isFollowUpExhausted()).isTrue();
        }

        @Test
        @DisplayName("Analyzer SKIP 권고 시 Step B 미호출 + analyzer_recommend_skip")
        void generateFollowUp_analyzerRecommendsSkip_skipsStepB() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("충분히 깊은 답변", analysisOf(RecommendedNextAction.SKIP)));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("analyzer_recommend_skip");
            assertThat(response.getAnswerText()).isEqualTo("충분히 깊은 답변");
            then(followUpQuestionWriter).shouldHaveNoInteractions();
            then(aiCallMetrics).should().incrementFollowUpSkip("analyzer_skip");
        }

        @Test
        @DisplayName("Analyzer SKIP 권고 시 base questionId 로 TurnCompletedEvent 를 발행한다")
        void generateFollowUp_analyzerRecommendsSkip_publishesEvent() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("충분히 깊은 답변", analysisOf(RecommendedNextAction.SKIP)));

            followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            then(followUpTransactionHandler).should()
                    .publishTurnCompletedEvent(eq(1L), any(FollowUpContext.class), any(TurnAnalysisResult.class), eq(50L), eq(0));
        }

        @Test
        @DisplayName("Step B 응답이 skip=false 인데 question 이 비어 있으면 PARSE_FAILED")
        void generateFollowUp_stepBNonSkipWithBlankQuestion_throwsParseFailed() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("답변", analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(), any(), any())).willReturn(stepBBlankQuestion());

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("AI_005"));
            then(followUpTransactionHandler).should(never())
                    .saveFollowUpResultAndPublishEvent(anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("Step B 자체 skip 반환 시 step_b_skip 카운터 증가 + skip 응답")
        void generateFollowUp_stepBSelfSkip_incrementsCounter() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("답변", analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(), any(), any())).willReturn(stepBSkip("답변이 main_question 과 무관"));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("답변이 main_question 과 무관");
            then(aiCallMetrics).should().incrementFollowUpSkip("step_b_skip");
        }

        @Test
        @DisplayName("Step B 자체 skip 반환 시 base questionId 로 TurnCompletedEvent 를 발행한다")
        void generateFollowUp_stepBSelfSkip_publishesEvent() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("답변", analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(), any(), any())).willReturn(stepBSkip("답변이 main_question 과 무관"));

            followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            then(followUpTransactionHandler).should()
                    .publishTurnCompletedEvent(eq(1L), any(FollowUpContext.class), any(TurnAnalysisResult.class), eq(50L), eq(0));
        }

        @Test
        @DisplayName("오디오 파일이 없으면 INTERVIEW_006 예외")
        void generateFollowUp_noAudioFile() {
            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("INTERVIEW_006"));
        }

        @Test
        @DisplayName("AudioTurnAnalyzer 호출 중 예외 발생 시 그대로 전파")
        void generateFollowUp_audioAnalyzerThrowsException_propagates() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 서비스를 사용할 수 없습니다."));

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("AI_005"));
        }

        @Test
        @DisplayName("진행 중이 아닌 면접에서 후속질문 생성 시 INTERVIEW_003 예외")
        void generateFollowUp_notInProgress() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L))
                    .willThrow(new BusinessException(HttpStatus.CONFLICT, "INTERVIEW_003", "면접이 진행 중이 아닙니다."));

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_003");
                    });
        }
    }

    @Nested
    @DisplayName("Resume 트랙 라우팅")
    class ResumeTrackRouting {

        @Test
        @DisplayName("skeleton 캐시가 있으면 resumeOrchestrator 로 위임한다 — CS 경로 미호출")
        void generateFollowUp_resumeTrack_skeleton_cached_delegatesToOrchestrator() {
            ResumeSkeleton skeleton = new ResumeSkeleton("r1", "h1", null, "backend", List.of(), java.util.Map.of());
            InterviewRuntimeState resumeState = new InterviewRuntimeState("JUNIOR", skeleton);
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 3));
            given(runtimeStateStore.getOrInit(any(), any())).willReturn(resumeState);
            given(runtimeStateStore.get(1L)).willReturn(resumeState);

            // skeleton 캐시 hit → isResumeTrack에서 getInterviewTypes 미호출, getDurationMinutes만 사용
            Interview interview = mock(Interview.class);
            given(interview.getDurationMinutes()).willReturn(30);
            given(interviewFinder.findById(1L)).willReturn(interview);

            InterviewPlan plan = mock(InterviewPlan.class);
            given(interviewPlanStore.findByInterviewId(1L)).willReturn(Optional.of(plan));

            given(resumeOrchestrator.processUserTurn(any(), anyInt(), any(), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(FollowUpResponse.builder().question("이력서 질문").presentToUser(true).build());

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.getQuestion()).isEqualTo("이력서 질문");
            then(resumeOrchestrator).should().processUserTurn(any(), anyInt(), any(), any(), any(), any(), any(), anyBoolean());
            then(audioTurnAnalyzer).shouldHaveNoInteractions();
            then(followUpQuestionWriter).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("skeleton 캐시 miss → RESUME_BASED 타입 면접이면 skeleton 재로드 후 위임한다")
        void generateFollowUp_resumeTrack_cacheMiss_reloadsSkeletonAndDelegates() {
            InterviewRuntimeState resumeState = new InterviewRuntimeState("JUNIOR", null);
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 3));
            given(runtimeStateStore.getOrInit(any(), any())).willReturn(resumeState);
            given(runtimeStateStore.get(1L)).willReturn(resumeState);
            given(runtimeStateStore.get(1L)).willReturn(resumeState);

            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.RESUME_BASED));
            given(interview.getDurationMinutes()).willReturn(30);
            given(interviewFinder.findById(1L)).willReturn(interview);

            ResumeSkeleton skeleton = new ResumeSkeleton("r1", "h1", null, "backend", List.of(), java.util.Map.of());
            given(resumeSkeletonStore.findByInterviewId(1L)).willReturn(Optional.of(skeleton));

            InterviewPlan plan = mock(InterviewPlan.class);
            given(interviewPlanStore.findByInterviewId(1L)).willReturn(Optional.of(plan));

            willAnswer(inv -> {
                java.util.function.Consumer<InterviewRuntimeState> mutator = inv.getArgument(1);
                mutator.accept(resumeState);
                return null;
            }).given(runtimeStateStore).update(eq(1L), any());

            given(resumeOrchestrator.processUserTurn(any(), anyInt(), any(), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(FollowUpResponse.builder().question("재로드 후 이력서 질문").presentToUser(true).build());

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.getQuestion()).isEqualTo("재로드 후 이력서 질문");
            then(resumeOrchestrator).should().processUserTurn(any(), anyInt(), any(), any(), any(), any(), any(), anyBoolean());
            then(audioTurnAnalyzer).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("plan 캐시/DB miss + skeleton DB hit이면 plan을 재생성하고 위임한다")
        void generateFollowUp_resumeTrack_planMissing_regeneratesPlanFromSkeleton() {
            InterviewRuntimeState resumeState = new InterviewRuntimeState("JUNIOR", null);
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 3));
            given(runtimeStateStore.getOrInit(any(), any())).willReturn(resumeState);
            given(runtimeStateStore.get(1L)).willReturn(resumeState);

            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.RESUME_BASED));
            given(interview.getDurationMinutes()).willReturn(30);
            given(interviewFinder.findById(1L)).willReturn(interview);

            ResumeSkeleton skeleton = new ResumeSkeleton("r1", "h1", null, "backend", List.of(), java.util.Map.of());
            given(resumeSkeletonStore.findByInterviewId(1L)).willReturn(Optional.of(skeleton));
            given(interviewPlanStore.findByInterviewId(1L)).willReturn(Optional.empty());

            InterviewPlan regenerated = mock(InterviewPlan.class);
            given(resumeInterviewPlanner.plan(skeleton, 30)).willReturn(regenerated);

            willAnswer(inv -> {
                java.util.function.Consumer<InterviewRuntimeState> mutator = inv.getArgument(1);
                mutator.accept(resumeState);
                return null;
            }).given(runtimeStateStore).update(eq(1L), any());

            given(resumeOrchestrator.processUserTurn(any(), anyInt(), any(), any(), any(), any(), any(), anyBoolean()))
                    .willReturn(FollowUpResponse.builder().question("복구된 plan 질문").presentToUser(true).build());

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.getQuestion()).isEqualTo("복구된 plan 질문");
            then(interviewPlanStore).should().save(1L, regenerated);
            then(resumeOrchestrator).should().processUserTurn(
                    eq(1L), eq(30), any(), any(), any(), eq(skeleton), eq(regenerated), eq(false));
        }

        @Test
        @DisplayName("RESUME_BASED 면접인데 skeleton도 없으면 409 준비 미완료 예외를 반환한다")
        void generateFollowUp_resumeTrack_skeletonMissing_returnsConflict() {
            InterviewRuntimeState resumeState = new InterviewRuntimeState("JUNIOR", null);
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 3));
            given(runtimeStateStore.getOrInit(any(), any())).willReturn(resumeState);
            given(runtimeStateStore.get(1L)).willReturn(resumeState);

            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.RESUME_BASED));
            given(interviewFinder.findById(1L)).willReturn(interview);
            given(resumeSkeletonStore.findByInterviewId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isNotEqualTo("RESUME_008");
                    });

            then(resumeOrchestrator).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("skeleton 캐시 miss + CS 타입 면접 → CS 경로로 처리된다")
        void generateFollowUp_csTrack_cacheMiss_routesToCsPath() {
            InterviewRuntimeState csState = new InterviewRuntimeState("JUNIOR", null);
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(runtimeStateStore.getOrInit(any(), any())).willReturn(csState);

            Interview csInterview = mock(Interview.class);
            given(csInterview.getInterviewTypes()).willReturn(Set.of(InterviewType.CS_FUNDAMENTAL));
            given(interviewFinder.findById(1L)).willReturn(csInterview);

            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("CS 답변", analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(), any(), any())).willReturn(stepBQuestion("CS 꼬리질문"));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.TECH_FOLLOWUP).questionText("CS 꼬리질문").orderIndex(1).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 99L);
            given(followUpTransactionHandler.saveFollowUpResultAndPublishEvent(any(), any(), any(), any()))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 1));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.getQuestion()).isEqualTo("CS 꼬리질문");
            then(resumeOrchestrator).shouldHaveNoInteractions();
        }

        private Interview stubResumeInterview() {
            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.RESUME_BASED));
            given(interview.getDurationMinutes()).willReturn(30);
            return interview;
        }
    }

    @Nested
    @DisplayName("[진행차단진단] WARN 로그")
    class BlockingDiagnosticsWarnLog {

        private ListAppender<ILoggingEvent> appender;
        private Logger targetLogger;

        @BeforeEach
        void attachAppender() {
            targetLogger = (Logger) LoggerFactory.getLogger(FollowUpService.class);
            appender = new ListAppender<>();
            appender.start();
            targetLogger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            targetLogger.detachAppender(appender);
        }

        @Test
        @DisplayName("analyzer-skip 분기 진입 시 track=STANDARD WARN 로그를 4개 키와 함께 기록")
        void analyzerSkip_logsBlockingDiagnosticsWarn() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("답변", analysisOf(RecommendedNextAction.SKIP)));

            followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            ILoggingEvent warn = findWarn(appender);
            assertThat(warn.getLevel()).isEqualTo(Level.WARN);
            String formatted = warn.getFormattedMessage();
            assertThat(formatted).contains("[진행차단진단]");
            assertThat(formatted).contains("interviewId=1");
            assertThat(formatted).contains("track=STANDARD");
            assertThat(formatted).contains("stage=standard-followup");
            assertThat(formatted).contains("reason=analyzer-skip");
        }

        @Test
        @DisplayName("step-b-skip 분기 진입 시 track=STANDARD WARN 로그를 4개 키와 함께 기록")
        void stepBSkip_logsBlockingDiagnosticsWarn() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn("답변", analysisOf(RecommendedNextAction.DEEP_DIVE)));
            given(followUpQuestionWriter.write(any(), any(), any())).willReturn(stepBSkip("답변이 main_question 과 무관"));

            followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            ILoggingEvent warn = findWarn(appender);
            assertThat(warn.getLevel()).isEqualTo(Level.WARN);
            String formatted = warn.getFormattedMessage();
            assertThat(formatted).contains("[진행차단진단]");
            assertThat(formatted).contains("interviewId=1");
            assertThat(formatted).contains("track=STANDARD");
            assertThat(formatted).contains("stage=standard-followup");
            assertThat(formatted).contains("reason=step-b-skip");
        }

        private ILoggingEvent findWarn(ListAppender<ILoggingEvent> appender) {
            return appender.list.stream()
                    .filter(e -> e.getLevel() == Level.WARN)
                    .filter(e -> e.getFormattedMessage().contains("[진행차단진단]"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("[진행차단진단] WARN 로그 미발견"));
        }
    }

}
