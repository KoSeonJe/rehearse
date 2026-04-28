package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.vo.TurnAnalysisResult;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.vo.AskedPerspectives;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.cache.InterviewPlanCache;
import com.rehearse.api.domain.resume.cache.ResumeSkeletonCache;
import com.rehearse.api.domain.resume.service.InterviewPlanStore;
import com.rehearse.api.domain.resume.service.ResumeSkeletonStore;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpService - Intent 분기 라우팅 (IntentDispatcher 위임)")
class FollowUpServiceIntentBranchTest {

    private FollowUpService followUpService;

    @Mock
    private AudioTurnAnalyzer audioTurnAnalyzer;

    @Mock
    private FollowUpQuestionWriter followUpQuestionWriter;

    @Mock
    private IntentDispatcher intentDispatcher;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Mock
    private InterviewRuntimeStateStore runtimeStateStore;

    @Mock
    private AiCallMetrics aiCallMetrics;

    @Mock
    private ResumeInterviewOrchestrator resumeOrchestrator;

    @Mock
    private ResumeSkeletonStore resumeSkeletonStore;

    @Mock
    private InterviewPlanStore interviewPlanStore;

    @Mock
    private ResumeSkeletonCache resumeSkeletonCache;

    @Mock
    private InterviewPlanCache interviewPlanCache;

    @Mock
    private InterviewFinder interviewFinder;

    private static final MockMultipartFile AUDIO_FILE =
            new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1, ReferenceType.MODEL_ANSWER, 2);

    private static final String MAIN_QUESTION = "HashMap의 해시 충돌 해결 방법을 설명해주세요.";
    private static final String ANSWER_TEXT = "체이닝 방식으로 해결합니다.";

    private FollowUpRequest buildRequest() {
        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", MAIN_QUESTION);
        return request;
    }

    private static AnswerAnalysis answerAnalysisDeepDive() {
        return new AnswerAnalysis(
                50L,
                List.of(new Claim("c", 3, EvidenceStrength.WEAK, "t")),
                List.of(),
                List.of(),
                3,
                RecommendedNextAction.DEEP_DIVE);
    }

    private static AnswerAnalysis emptyAnalysis() {
        return AnswerAnalysis.empty(50L);
    }

    private static TurnAnalysisResult turn(IntentType intent, String answerText, AnswerAnalysis analysis) {
        return new TurnAnalysisResult(answerText, IntentResult.of(intent, 0.95, "test"), analysis);
    }

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        followUpService = new FollowUpService(
                audioTurnAnalyzer, followUpQuestionWriter, intentDispatcher,
                followUpTransactionHandler, runtimeStateStore, aiCallMetrics,
                resumeOrchestrator, resumeSkeletonStore, interviewPlanStore,
                resumeSkeletonCache, interviewPlanCache, interviewFinder,
                eventPublisher);

        lenient().when(followUpTransactionHandler.loadFollowUpContext(anyLong(), anyLong(), anyLong())).thenReturn(CONTEXT);
        lenient().when(runtimeStateStore.getOrInit(any(), any()))
                .thenReturn(new InterviewRuntimeState("JUNIOR", null));

        // skeleton=null 경로에서 interviewFinder 호출 시 CS interview 반환
        Interview csDefault = mock(Interview.class);
        lenient().when(csDefault.getInterviewTypes()).thenReturn(java.util.Set.of(InterviewType.CS_FUNDAMENTAL));
        lenient().when(interviewFinder.findById(any())).thenReturn(csDefault);
    }

    @Nested
    @DisplayName("ANSWER 분기 — Step B v3 경로")
    class AnswerBranch {

        @Test
        @DisplayName("ANSWER 의도이면 Step B 호출 + DB 저장 + presentToUser=true")
        void generateFollowUp_answer_savesAndReturnsPresentToUserTrue() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn(IntentType.ANSWER, ANSWER_TEXT, answerAnalysisDeepDive()));

            GeneratedFollowUp stepB = new GeneratedFollowUp();
            ReflectionTestUtils.setField(stepB, "question", "꼬리질문 텍스트");
            ReflectionTestUtils.setField(stepB, "ttsQuestion", "꼬리질문 텍스트");
            ReflectionTestUtils.setField(stepB, "type", "DEEP_DIVE");
            ReflectionTestUtils.setField(stepB, "skip", Boolean.FALSE);
            ReflectionTestUtils.setField(stepB, "answerText", "x");
            given(followUpQuestionWriter.write(any(FollowUpGenerationRequest.class), any(AnswerAnalysis.class), any(AskedPerspectives.class)))
                    .willReturn(stepB);

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("꼬리질문 텍스트")
                    .orderIndex(1)
                    .build();
            ReflectionTestUtils.setField(savedQuestion, "id", 100L);
            given(followUpTransactionHandler.saveFollowUpResult(anyLong(), any(), anyInt()))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 1));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.isSkip()).isFalse();
            assertThat(response.isPresentToUser()).isTrue();
            assertThat(response.getQuestionId()).isEqualTo(100L);
            assertThat(response.isFollowUpExhausted()).isFalse();
            then(intentDispatcher).should(never()).dispatch(any(), any());
        }
    }

    @Nested
    @DisplayName("Strategy dispatch — IntentDispatcher 위임")
    class StrategyDispatch {

        @Test
        @DisplayName("OFF_TOPIC 의도이면 dispatcher 호출 + DB 저장 건너뜀")
        void generateFollowUp_offTopic_delegatesToDispatcher() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn(IntentType.OFF_TOPIC, "시간이 얼마나 남았어요?", emptyAnalysis()));

            FollowUpResponse offTopicResponse = FollowUpResponse.builder()
                    .question("OFF_TOPIC 응답").skip(true).skipReason("OFF_TOPIC").presentToUser(true).build();
            given(intentDispatcher.dispatch(any(), any())).willReturn(offTopicResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
            assertThat(response.isPresentToUser()).isTrue();
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
            then(followUpQuestionWriter).shouldHaveNoInteractions();
            then(aiCallMetrics).should().incrementFollowUpSkip("intent_off_topic");
        }

        @Test
        @DisplayName("CLARIFY_REQUEST 의도이면 dispatcher 호출")
        void generateFollowUp_clarifyRequest_delegatesToDispatcher() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn(IntentType.CLARIFY_REQUEST, "그게 무슨 뜻인가요?", emptyAnalysis()));

            FollowUpResponse clarifyResponse = FollowUpResponse.builder()
                    .question("재설명 응답").skip(true).skipReason("CLARIFY_REQUEST").presentToUser(true).build();
            given(intentDispatcher.dispatch(any(), any())).willReturn(clarifyResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("CLARIFY_REQUEST");
            then(intentDispatcher).should().dispatch(any(), any());
            then(followUpQuestionWriter).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("GIVE_UP 의도이면 dispatcher 호출")
        void generateFollowUp_giveUp_delegatesToDispatcher() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any(AskedPerspectives.class)))
                    .willReturn(turn(IntentType.GIVE_UP, "모르겠어요.", emptyAnalysis()));

            FollowUpResponse giveUpResponse = FollowUpResponse.builder()
                    .question("힌트 제공").skip(true).skipReason("GIVE_UP").presentToUser(true).build();
            given(intentDispatcher.dispatch(any(), any())).willReturn(giveUpResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            then(intentDispatcher).should().dispatch(any(), any());
        }
    }
}
