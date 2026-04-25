package com.rehearse.api.domain.interview.service;

import com.rehearse.api.global.config.IntentClassifierProperties;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.prompt.IntentClassifierPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntentClassifier - 발화 의도 분류")
class IntentClassifierTest {

    private IntentClassifier intentClassifier;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    @Mock
    private IntentClassifierPromptBuilder promptBuilder;

    private final IntentClassifierProperties properties = new IntentClassifierProperties(0.7, 3);

    private static final ChatResponse DUMMY_RESPONSE =
            new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);

    @BeforeEach
    void setUp() {
        intentClassifier = new IntentClassifier(aiClient, aiResponseParser, promptBuilder, properties);
        lenient().when(promptBuilder.buildSystemPrompt()).thenReturn("system-prompt");
        lenient().when(promptBuilder.buildUserPrompt(any(), any(), any())).thenReturn("user-prompt");
        lenient().when(aiClient.chat(any(ChatRequest.class))).thenReturn(DUMMY_RESPONSE);
    }

    private void givenParsedIntent(String intentRaw, double confidence, String reasoning) {
        given(aiResponseParser.parseOrRetry(
                any(ChatResponse.class),
                eq(IntentClassifier.IntentClassificationResponse.class),
                any(AiClient.class),
                any(ChatRequest.class)
        )).willReturn(new IntentClassifier.IntentClassificationResponse(intentRaw, confidence, reasoning));
    }

    @Nested
    @DisplayName("4-intent 분류 정상 경로")
    class NormalClassification {

        @Test
        @DisplayName("ANSWER 의도 + 높은 신뢰도이면 ANSWER IntentResult를 반환한다")
        void classify_answer_returnsAnswerIntent() {
            givenParsedIntent("ANSWER", 0.95, "실질적인 답변 시도");

            IntentResult result = intentClassifier.classify(
                    "JVM GC를 설명해주세요.", "Young/Old Generation으로 나뉩니다.", null);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.confidence()).isEqualTo(0.95);
            assertThat(result.reasoning()).isEqualTo("실질적인 답변 시도");
            assertThat(result.fallback()).isFalse();
        }

        @Test
        @DisplayName("CLARIFY_REQUEST 의도이면 CLARIFY_REQUEST IntentResult를 반환한다")
        void classify_clarifyRequest_returnsClarifyIntent() {
            givenParsedIntent("CLARIFY_REQUEST", 0.92, "질문 재설명 요청");

            IntentResult result = intentClassifier.classify(
                    "서비스 디스커버리를 설명해주세요.", "어떤 방식으로 설명할까요?", null);

            assertThat(result.type()).isEqualTo(IntentType.CLARIFY_REQUEST);
            assertThat(result.confidence()).isEqualTo(0.92);
        }

        @Test
        @DisplayName("GIVE_UP 의도이면 GIVE_UP IntentResult를 반환한다")
        void classify_giveUp_returnsGiveUpIntent() {
            givenParsedIntent("GIVE_UP", 0.97, "포기 명시");

            IntentResult result = intentClassifier.classify(
                    "B-Tree와 B+Tree 차이를 설명해주세요.", "모르겠어요, 패스할게요.", null);

            assertThat(result.type()).isEqualTo(IntentType.GIVE_UP);
            assertThat(result.confidence()).isEqualTo(0.97);
        }

        @Test
        @DisplayName("OFF_TOPIC 의도이면 OFF_TOPIC IntentResult를 반환한다")
        void classify_offTopic_returnsOffTopicIntent() {
            givenParsedIntent("OFF_TOPIC", 0.99, "메타 발화");

            IntentResult result = intentClassifier.classify(
                    "HashMap 충돌 해결을 설명해주세요.", "시간이 얼마나 남았어요?", null);

            assertThat(result.type()).isEqualTo(IntentType.OFF_TOPIC);
            assertThat(result.confidence()).isEqualTo(0.99);
        }

        @Test
        @DisplayName("LLM이 소문자 intent를 반환해도 정상 분류한다")
        void classify_lowercaseIntent_parsesCorrectly() {
            givenParsedIntent("answer", 0.90, "소문자 반환");

            IntentResult result = intentClassifier.classify("질문", "답변", null);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.fallback()).isFalse();
        }

        @Test
        @DisplayName("LLM이 알 수 없는 intent 값을 반환하면 forceAnswer fallback을 반환한다")
        void classify_unknownIntent_returnsForceAnswer() {
            givenParsedIntent("UNKNOWN_INTENT", 0.95, "알 수 없음");

            IntentResult result = intentClassifier.classify("질문", "답변", null);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.fallback()).isTrue();
            assertThat(result.confidence()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("낮은 신뢰도 및 실패 fallback")
    class FallbackBehavior {

        @Test
        @DisplayName("신뢰도가 fallbackOnLowConfidence 미만이면 forceAnswer를 반환한다")
        void classify_lowConfidence_returnsForceAnswer() {
            givenParsedIntent("OFF_TOPIC", 0.5, "불확실");

            IntentResult result = intentClassifier.classify("질문", "답변", null);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.confidence()).isEqualTo(0.0);
            assertThat(result.reasoning()).isEqualTo("low confidence or error fallback");
            assertThat(result.fallback()).isTrue();
        }

        @Test
        @DisplayName("신뢰도가 fallbackOnLowConfidence와 정확히 같으면 정상 분류한다")
        void classify_confidenceExactlyAtThreshold_classifiesNormally() {
            givenParsedIntent("GIVE_UP", 0.7, "임계값 정확히 일치");

            IntentResult result = intentClassifier.classify("질문", "답변", null);

            assertThat(result.type()).isEqualTo(IntentType.GIVE_UP);
        }

        @Test
        @DisplayName("AI 호출 중 예외 발생 시 forceAnswer를 반환한다")
        void classify_aiException_returnsForceAnswer() {
            given(aiClient.chat(any(ChatRequest.class)))
                    .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 서비스 불가"));

            IntentResult result = intentClassifier.classify("질문", "답변", null);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.reasoning()).isEqualTo("low confidence or error fallback");
            assertThat(result.fallback()).isTrue();
        }

        @Test
        @DisplayName("JSON 파싱 최종 실패 시 forceAnswer를 반환한다")
        void classify_parseFailed_returnsForceAnswer() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "파싱 실패"));

            IntentResult result = intentClassifier.classify("질문", "답변", null);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
            assertThat(result.reasoning()).isEqualTo("low confidence or error fallback");
            assertThat(result.fallback()).isTrue();
        }

        @Test
        @DisplayName("previousExchanges가 있어도 정상적으로 분류한다")
        void classify_withPreviousExchanges_works() {
            List<FollowUpExchange> exchanges = List.of(
                    new FollowUpExchange("이전 질문", "이전 답변")
            );
            given(promptBuilder.buildUserPrompt(any(), any(), eq(exchanges))).willReturn("user-prompt-with-exchanges");
            givenParsedIntent("ANSWER", 0.88, "답변 시도");

            IntentResult result = intentClassifier.classify("질문", "답변", exchanges);

            assertThat(result.type()).isEqualTo(IntentType.ANSWER);
        }
    }
}
