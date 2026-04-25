package com.rehearse.api.domain.interview.service;

import com.rehearse.api.global.config.IntentClassifierProperties;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
@DisplayName("FollowUpService - Intent 분기 라우팅")
class FollowUpServiceIntentBranchTest {

    @InjectMocks
    private FollowUpService followUpService;

    @Mock
    private AiClient aiClient;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Mock
    private IntentClassifier intentClassifier;

    @Mock
    private IntentClassifierProperties intentClassifierProperties;

    @Mock
    private OffTopicResponseHandler offTopicResponseHandler;

    @Mock
    private ClarifyResponseHandler clarifyResponseHandler;

    @Mock
    private GiveUpResponseHandler giveUpResponseHandler;

    @Mock
    private OffTopicEscalationDetector offTopicEscalationDetector;

    private static final MockMultipartFile AUDIO_FILE =
            new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1, ReferenceType.MODEL_ANSWER);

    private static final String MAIN_QUESTION = "HashMap의 해시 충돌 해결 방법을 설명해주세요.";
    private static final String ANSWER_TEXT = "체이닝 방식으로 해결합니다.";

    private FollowUpRequest buildRequest() {
        return buildRequest(null);
    }

    private FollowUpRequest buildRequest(List<FollowUpExchange> exchanges) {
        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", MAIN_QUESTION);
        ReflectionTestUtils.setField(request, "previousExchanges", exchanges);
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
        lenient().when(followUpTransactionHandler.loadFollowUpContext(anyLong(), anyLong(), anyLong())).thenReturn(CONTEXT);
        lenient().when(intentClassifierProperties.offTopicConsecutiveLimit()).thenReturn(3);
        lenient().when(offTopicEscalationDetector.countRecentConsecutive(any())).thenReturn(0);
        lenient().when(offTopicEscalationDetector.shouldEscalate(anyInt(), anyInt())).thenReturn(false);
    }

    @Nested
    @DisplayName("ANSWER 분기 — 기존 경로 그대로")
    class AnswerBranch {

        @Test
        @DisplayName("ANSWER 의도이면 DB에 저장하고 presentToUser=true인 FollowUpResponse를 반환한다")
        void generateFollowUp_answer_savesAndReturnsPresentToUserTrue() {
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문 텍스트", ANSWER_TEXT);
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class))).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.ANSWER, 0.95, "실질적 답변"));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("꼬리질문 텍스트")
                    .orderIndex(1)
                    .build();
            ReflectionTestUtils.setField(savedQuestion, "id", 100L);
            given(followUpTransactionHandler.saveFollowUpResult(anyLong(), any(), anyInt())).willReturn(savedQuestion);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.isSkip()).isFalse();
            assertThat(response.isPresentToUser()).isTrue();
            assertThat(response.getQuestionId()).isEqualTo(100L);
            assertThat(response.getQuestion()).isEqualTo("꼬리질문 텍스트");
            then(followUpTransactionHandler).should().saveFollowUpResult(anyLong(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("OFF_TOPIC 분기")
    class OffTopicBranch {

        @Test
        @DisplayName("OFF_TOPIC 의도이면 offTopicResponseHandler를 호출하고 presentToUser=true를 반환한다")
        void generateFollowUp_offTopic_callsHandlerAndSkipsSave() {
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "시간이 얼마나 남았어요?");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.OFF_TOPIC, 0.99, "메타 발화"));

            FollowUpResponse offTopicResponse = FollowUpResponse.builder()
                    .question("OFF_TOPIC 응답").skip(true).skipReason("OFF_TOPIC").presentToUser(true).build();
            given(offTopicResponseHandler.handle(anyLong(), anyInt(), any(), any())).willReturn(offTopicResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.isPresentToUser()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
        }

        @Test
        @DisplayName("OFF_TOPIC 연속 3회이면 GIVE_UP으로 escalation하고 presentToUser=true를 반환한다")
        void generateFollowUp_offTopicThreeConsecutive_escalatesToGiveUp() {
            String marker = OffTopicResponseHandler.OFF_TOPIC_CONNECTOR;
            List<FollowUpExchange> exchanges = List.of(
                    new FollowUpExchange("리드인1 " + marker + " " + MAIN_QUESTION, "잡담1"),
                    new FollowUpExchange("리드인2 " + marker + " " + MAIN_QUESTION, "잡담2")
            );

            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "또 잡담");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.OFF_TOPIC, 0.99, "메타 발화"));
            given(offTopicEscalationDetector.countRecentConsecutive(any())).willReturn(2);
            given(offTopicEscalationDetector.shouldEscalate(2, 3)).willReturn(true);

            FollowUpResponse giveUpResponse = FollowUpResponse.builder()
                    .question("GIVE_UP escalation").skip(true).skipReason("GIVE_UP").presentToUser(true).build();
            given(giveUpResponseHandler.handle(any(), any(), any())).willReturn(giveUpResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(exchanges), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            assertThat(response.isPresentToUser()).isTrue();
            then(giveUpResponseHandler).should().handle(any(), any(), any());
            then(offTopicResponseHandler).should(never()).handle(anyLong(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("OFF_TOPIC 연속 2회(임계 미만)이면 offTopicResponseHandler를 호출한다")
        void generateFollowUp_offTopicTwoConsecutive_callsOffTopicHandler() {
            String marker = OffTopicResponseHandler.OFF_TOPIC_CONNECTOR;
            List<FollowUpExchange> exchanges = List.of(
                    new FollowUpExchange("리드인1 " + marker + " " + MAIN_QUESTION, "잡담1")
            );

            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "또 잡담");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.OFF_TOPIC, 0.99, "메타 발화"));
            given(offTopicEscalationDetector.countRecentConsecutive(any())).willReturn(1);
            given(offTopicEscalationDetector.shouldEscalate(1, 3)).willReturn(false);

            FollowUpResponse offTopicResponse = FollowUpResponse.builder()
                    .question("OFF_TOPIC 응답").skip(true).skipReason("OFF_TOPIC").presentToUser(true).build();
            given(offTopicResponseHandler.handle(anyLong(), anyInt(), any(), any())).willReturn(offTopicResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(exchanges), AUDIO_FILE);

            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
            then(offTopicResponseHandler).should().handle(anyLong(), anyInt(), any(), any());
            then(giveUpResponseHandler).should(never()).handle(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("CLARIFY_REQUEST 분기")
    class ClarifyBranch {

        @Test
        @DisplayName("CLARIFY_REQUEST 의도이면 clarifyResponseHandler를 호출하고 presentToUser=true를 반환한다")
        void generateFollowUp_clarifyRequest_callsHandlerAndReturnsPresentToUserTrue() {
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "그게 무슨 뜻인가요?");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.CLARIFY_REQUEST, 0.92, "재설명 요청"));

            FollowUpResponse clarifyResponse = FollowUpResponse.builder()
                    .question("재설명 응답").skip(true).skipReason("CLARIFY_REQUEST").presentToUser(true).build();
            given(clarifyResponseHandler.handle(any(), any(), any())).willReturn(clarifyResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.isPresentToUser()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("CLARIFY_REQUEST");
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("GIVE_UP 분기")
    class GiveUpBranch {

        @Test
        @DisplayName("GIVE_UP 의도이면 giveUpResponseHandler를 호출하고 presentToUser=true를 반환한다")
        void generateFollowUp_giveUp_callsHandlerAndReturnsPresentToUserTrue() {
            GeneratedFollowUp followUp = buildFollowUp("꼬리질문", "모르겠어요.");
            given(aiClient.generateFollowUpWithAudio(any(), any())).willReturn(followUp);
            given(intentClassifier.classify(any(), any(), any()))
                    .willReturn(IntentResult.of(IntentType.GIVE_UP, 0.97, "포기 명시"));

            FollowUpResponse giveUpResponse = FollowUpResponse.builder()
                    .question("힌트 제공").skip(true).skipReason("GIVE_UP").presentToUser(true).build();
            given(giveUpResponseHandler.handle(any(), any(), any())).willReturn(giveUpResponse);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, buildRequest(), AUDIO_FILE);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.isPresentToUser()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("AI 자체 skip 분기")
    class AiSkipBranch {

        @Test
        @DisplayName("AI가 답변 불충분으로 skip 신호를 보내면 presentToUser=false를 반환한다")
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
