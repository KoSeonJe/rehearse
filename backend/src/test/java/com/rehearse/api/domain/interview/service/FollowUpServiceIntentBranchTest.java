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
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpService - Intent 분기 라우팅 (Strategy dispatch)")
class FollowUpServiceIntentBranchTest {

    private FollowUpService followUpService;

    @Mock
    private AiClient aiClient;

    @Mock
    private FollowUpPromptBuilder followUpPromptBuilder;

    @Mock
    private AnswerAnalyzer answerAnalyzer;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Mock
    private IntentClassifier intentClassifier;

    @Mock
    private OffTopicResponseHandler offTopicResponseHandler;

    @Mock
    private ClarifyResponseHandler clarifyResponseHandler;

    @Mock
    private GiveUpResponseHandler giveUpResponseHandler;

    private AiResponseParser aiResponseParser;

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

    private GeneratedFollowUp buildFollowUp(String question, String answerText) {
        GeneratedFollowUp followUp = new GeneratedFollowUp();
        ReflectionTestUtils.setField(followUp, "question", question);
        ReflectionTestUtils.setField(followUp, "reason", "깊이 확인");
        ReflectionTestUtils.setField(followUp, "type", "DEEP_DIVE");
        ReflectionTestUtils.setField(followUp, "answerText", answerText);
        return followUp;
    }

    @BeforeEach
    void setUp() {
        aiResponseParser = new AiResponseParser(new ObjectMapper());

        lenient().when(offTopicResponseHandler.supports()).thenReturn(IntentType.OFF_TOPIC);
        lenient().when(clarifyResponseHandler.supports()).thenReturn(IntentType.CLARIFY_REQUEST);
        lenient().when(giveUpResponseHandler.supports()).thenReturn(IntentType.GIVE_UP);

        followUpService = new FollowUpService(
                aiClient, aiResponseParser, followUpPromptBuilder, answerAnalyzer,
                followUpTransactionHandler, intentClassifier,
                List.of(offTopicResponseHandler, clarifyResponseHandler, giveUpResponseHandler));
        ReflectionTestUtils.invokeMethod(followUpService, "registerHandlers");

        lenient().when(followUpTransactionHandler.loadFollowUpContext(anyLong(), anyLong(), anyLong())).thenReturn(CONTEXT);
        lenient().when(followUpPromptBuilder.buildSystemPrompt(any())).thenReturn("system");
        lenient().when(followUpPromptBuilder.buildUserPromptWithAnalysis(any(), any(), any())).thenReturn("user");
    }

    @Nested
    @DisplayName("ANSWER 분기 — 기존 경로 그대로")
    class AnswerBranch {

        @Test
        @DisplayName("ANSWER 의도이면 Step A→Step B 경로로 저장하고 presentToUser=true인 응답을 반환한다")
        void generateFollowUp_answer_savesAndReturnsPresentToUserTrue() {
            GeneratedFollowUp stt = buildFollowUp("ignored", ANSWER_TEXT);
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class))).willReturn(stt);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.95, "실질적 답변"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(new AnswerAnalysis(
                            50L,
                            List.of(new Claim("c", 3, EvidenceStrength.WEAK, "t")),
                            List.of(),
                            List.of(),
                            3,
                            RecommendedNextAction.DEEP_DIVE));

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
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "시간이 얼마나 남았어요?");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.OFF_TOPIC, 0.99, "메타 발화"));

            FollowUpResponse offTopicResponse = FollowUpResponse.builder()
                    .question("OFF_TOPIC 응답").skip(true).skipReason("OFF_TOPIC").presentToUser(true).build();
            given(offTopicResponseHandler.handle(any(IntentBranchInput.class))).willReturn(offTopicResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
            assertThat(response.isPresentToUser()).isTrue();
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
            then(offTopicResponseHandler).should().handle(any(IntentBranchInput.class));
        }

        @Test
        @DisplayName("CLARIFY_REQUEST 의도이면 clarifyResponseHandler를 호출한다")
        void generateFollowUp_clarifyRequest_delegatesToClarifyHandler() {
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "그게 무슨 뜻인가요?");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.CLARIFY_REQUEST, 0.92, "재설명 요청"));

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
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "모르겠어요.");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.GIVE_UP, 0.97, "포기 명시"));

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

    @Nested
    @DisplayName("AI 자체 skip 분기")
    class AiSkipBranch {

        @Test
        @DisplayName("AI가 답변 불충분으로 skip 신호를 보내면 분류기 호출 없이 presentToUser=false를 반환한다")
        void generateFollowUp_aiSkip_returnsPresentToUserFalse() {
            GeneratedFollowUp skippedFollowUp = new GeneratedFollowUp();
            ReflectionTestUtils.setField(skippedFollowUp, "skip", true);
            ReflectionTestUtils.setField(skippedFollowUp, "skipReason", "INSUFFICIENT_ANSWER");
            ReflectionTestUtils.setField(skippedFollowUp, "answerText", "짧은 답변");

            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(skippedFollowUp);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.isPresentToUser()).isFalse();
            assertThat(response.getSkipReason()).isEqualTo("INSUFFICIENT_ANSWER");
            then(intentClassifier).should(never()).classify(any(), any(), any());
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
        }
    }
}
