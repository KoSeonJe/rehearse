package com.rehearse.api.domain.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.dto.TurnAnalysisResult;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpService - Intent 분기 라우팅 (AudioTurnAnalyzer 결과 dispatch)")
class FollowUpServiceIntentBranchTest {

    private FollowUpService followUpService;

    @Mock
    private AiClient aiClient;

    @Mock
    private AudioTurnAnalyzer audioTurnAnalyzer;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Mock
    private OffTopicResponseHandler offTopicResponseHandler;

    @Mock
    private ClarifyResponseHandler clarifyResponseHandler;

    @Mock
    private GiveUpResponseHandler giveUpResponseHandler;

    @Mock
    private InterviewContextBuilder contextBuilder;

    @Mock
    private InterviewRuntimeStateStore runtimeStateStore;

    @Mock
    private AiCallMetrics aiCallMetrics;

    private AiResponseParser aiResponseParser;

    private static final MockMultipartFile AUDIO_FILE =
            new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1, ReferenceType.MODEL_ANSWER, 2);

    private static final String MAIN_QUESTION = "HashMap의 해시 충돌 해결 방법을 설명해주세요.";
    private static final String ANSWER_TEXT = "체이닝 방식으로 해결합니다.";

    private static final BuiltContext STUB_CONTEXT = new BuiltContext(
            List.of(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, "system"),
                    ChatMessage.of(ChatMessage.Role.USER, "user")),
            100,
            Map.of("L1", 80, "L4", 20, "total", 100)
    );

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
        return new AnswerAnalysis(50L, List.of(), List.of(), List.of(), 1, RecommendedNextAction.CLARIFICATION);
    }

    private static TurnAnalysisResult turnResult(IntentType intent, String answerText, AnswerAnalysis analysis) {
        return new TurnAnalysisResult(answerText, IntentResult.of(intent, 0.95, "test"), analysis);
    }

    @BeforeEach
    void setUp() {
        aiResponseParser = new AiResponseParser(new ObjectMapper());

        lenient().when(offTopicResponseHandler.supports()).thenReturn(IntentType.OFF_TOPIC);
        lenient().when(clarifyResponseHandler.supports()).thenReturn(IntentType.CLARIFY_REQUEST);
        lenient().when(giveUpResponseHandler.supports()).thenReturn(IntentType.GIVE_UP);

        followUpService = new FollowUpService(
                aiClient, aiResponseParser, audioTurnAnalyzer,
                followUpTransactionHandler,
                List.of(offTopicResponseHandler, clarifyResponseHandler, giveUpResponseHandler),
                contextBuilder, runtimeStateStore, aiCallMetrics);
        ReflectionTestUtils.invokeMethod(followUpService, "registerHandlers");

        lenient().when(followUpTransactionHandler.loadFollowUpContext(anyLong(), anyLong(), anyLong())).thenReturn(CONTEXT);
        lenient().when(contextBuilder.build(any(ContextBuildRequest.class))).thenReturn(STUB_CONTEXT);
        lenient().when(runtimeStateStore.getOrInit(any(), any()))
                .thenReturn(new InterviewRuntimeState("JUNIOR", null));
    }

    @Nested
    @DisplayName("ANSWER 분기 — Step B v3 경로")
    class AnswerBranch {

        @Test
        @DisplayName("ANSWER 의도이면 Step B 경로로 저장하고 presentToUser=true 인 응답을 반환한다")
        void generateFollowUp_answer_savesAndReturnsPresentToUserTrue() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(turnResult(IntentType.ANSWER, ANSWER_TEXT, answerAnalysisDeepDive()));

            String stepBJson = """
                    {"skip": false, "answerText": "x", "question": "꼬리질문 텍스트", "ttsQuestion": "꼬리질문 텍스트",
                     "reason": "r", "type": "DEEP_DIVE", "modelAnswer": "m",
                     "target_claim_idx": 0, "selected_perspective": null}
                    """;
            given(aiClient.chat(any(ChatRequest.class)))
                    .willReturn(new ChatResponse(stepBJson, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false));

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
            then(followUpTransactionHandler).should().saveFollowUpResult(anyLong(), any(), anyInt());
            then(offTopicResponseHandler).should(never()).handle(any(IntentBranchInput.class));
            then(clarifyResponseHandler).should(never()).handle(any(IntentBranchInput.class));
            then(giveUpResponseHandler).should(never()).handle(any(IntentBranchInput.class));
        }
    }

    @Nested
    @DisplayName("Strategy dispatch — IntentResponseHandler 위임")
    class StrategyDispatch {

        @Test
        @DisplayName("OFF_TOPIC 의도이면 offTopicResponseHandler를 호출하고 DB 저장을 건너뛴다")
        void generateFollowUp_offTopic_delegatesToOffTopicHandler() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(turnResult(IntentType.OFF_TOPIC, "시간이 얼마나 남았어요?", emptyAnalysis()));

            FollowUpResponse offTopicResponse = FollowUpResponse.builder()
                    .question("OFF_TOPIC 응답").skip(true).skipReason("OFF_TOPIC").presentToUser(true).build();
            given(offTopicResponseHandler.handle(any(IntentBranchInput.class))).willReturn(offTopicResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
            assertThat(response.isPresentToUser()).isTrue();
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
            then(offTopicResponseHandler).should().handle(any(IntentBranchInput.class));
            then(aiCallMetrics).should().incrementFollowUpSkip("intent_off_topic");
        }

        @Test
        @DisplayName("CLARIFY_REQUEST 의도이면 clarifyResponseHandler를 호출한다")
        void generateFollowUp_clarifyRequest_delegatesToClarifyHandler() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(turnResult(IntentType.CLARIFY_REQUEST, "그게 무슨 뜻인가요?", emptyAnalysis()));

            FollowUpResponse clarifyResponse = FollowUpResponse.builder()
                    .question("재설명 응답").skip(true).skipReason("CLARIFY_REQUEST").presentToUser(true).build();
            given(clarifyResponseHandler.handle(any(IntentBranchInput.class))).willReturn(clarifyResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("CLARIFY_REQUEST");
            assertThat(response.isPresentToUser()).isTrue();
            then(clarifyResponseHandler).should().handle(any(IntentBranchInput.class));
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
        }

        @Test
        @DisplayName("GIVE_UP 의도이면 giveUpResponseHandler를 호출한다")
        void generateFollowUp_giveUp_delegatesToGiveUpHandler() {
            given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(turnResult(IntentType.GIVE_UP, "모르겠어요.", emptyAnalysis()));

            FollowUpResponse giveUpResponse = FollowUpResponse.builder()
                    .question("힌트 제공").skip(true).skipReason("GIVE_UP").presentToUser(true).build();
            given(giveUpResponseHandler.handle(any(IntentBranchInput.class))).willReturn(giveUpResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            assertThat(response.isPresentToUser()).isTrue();
            then(giveUpResponseHandler).should().handle(any(IntentBranchInput.class));
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
        }
    }
}
