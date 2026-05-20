package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RubricScoringAdapter")
class RubricScoringAdapterTest {

    private static final Long INTERVIEW_ID = 100L;
    private static final Long QUESTION_ID = 200L;
    private static final String USER_ANSWER = "저는 작년에 결제 모듈을 리팩토링하여 TPS 10000 을 달성했습니다";

    @Mock
    private AiResponseParser responseParser;

    @Mock
    private AiClient aiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MeterRegistry meterRegistry;
    private RubricScorerResponseValidator validator;
    private RubricScoringAdapter adapter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        validator = new RubricScorerResponseValidator();
        adapter = new RubricScoringAdapter(responseParser, objectMapper, validator, meterRegistry);
    }

    @Nested
    @DisplayName("1차 응답 정상")
    class FirstCallValid {

        @Test
        @DisplayName("한국어 observation + verbatim evidence 면 retry 없이 적재")
        void validFirstCall_persistsWithoutRetry() {
            String json = """
                    {"technical_depth":{"score":2,"observation":"구체적 수치 인용","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            given(aiClient.chat(any())).willReturn(mockChatResponse("first"));
            given(responseParser.extractJson("first")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            verify(aiClient, times(1)).chat(any());
            assertThat(result.scoredDimensions()).containsExactly("technical_depth");
            assertThat(result.dimensionScores().get("technical_depth").score()).isEqualTo(2);
            assertThat(retryFailedCount("technical_depth", "observation")).isZero();
        }
    }

    @Nested
    @DisplayName("1차 위배 → retry 통과")
    class RetrySuccess {

        @Test
        @DisplayName("영어 observation 1차 위배 → retry 한국어 정상 → 적재 + 메트릭 미증가")
        void englishObservation_retrySucceeds() {
            String firstJson = """
                    {"technical_depth":{"score":2,"observation":"good technical depth","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            String retryJson = """
                    {"technical_depth":{"score":2,"observation":"구체적 수치 인용","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            given(aiClient.chat(any()))
                    .willReturn(mockChatResponse("first"))
                    .willReturn(mockChatResponse("retry"));
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            verify(aiClient, times(2)).chat(any());
            assertThat(result.scoredDimensions()).containsExactly("technical_depth");
            assertThat(result.dimensionScores().get("technical_depth").observation()).isEqualTo("구체적 수치 인용");
            assertThat(retryFailedCount("technical_depth", "observation")).isZero();
        }
    }

    @Nested
    @DisplayName("1차/2차 모두 위배")
    class RetryFails {

        @Test
        @DisplayName("evidence_quote 가 두 번 모두 userAnswer 미포함 → 해당 dimension omit + 메트릭 +1 + 로그 라벨 정확")
        void evidenceNotInAnswer_retryFails_omits() {
            String firstJson = """
                    {"technical_depth":{"score":2,"observation":"구체적 답변","evidence_quote":"팀에서 했어요 수준"}}
                    """;
            String retryJson = """
                    {"technical_depth":{"score":2,"observation":"구체적 답변","evidence_quote":"루브릭 정의문"}}
                    """;
            given(aiClient.chat(any()))
                    .willReturn(mockChatResponse("first"))
                    .willReturn(mockChatResponse("retry"));
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(result.dimensionScores().get("technical_depth").observation())
                    .startsWith("retry_failed:");
            assertThat(retryFailedCount("technical_depth", "evidence_quote")).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("retry 응답 차원 누락")
    class RetryMissingDimension {

        @Test
        @DisplayName("retry 응답에서 위배 dimension 누락 → field=score 메트릭 +1 + 해당 dimension omit")
        void retryResponseMissingDimension_recordsScoreField() {
            String firstJson = """
                    {"technical_depth":{"score":2,"observation":"english only","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            String retryJson = "{}";
            given(aiClient.chat(any()))
                    .willReturn(mockChatResponse("first"))
                    .willReturn(mockChatResponse("retry"));
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(retryFailedCount("technical_depth", "score")).isEqualTo(1.0);
            assertThat(retryFailedCount("technical_depth", "observation")).isZero();
        }
    }

    @Nested
    @DisplayName("dimension 단위 fault isolation")
    class FaultIsolation {

        @Test
        @DisplayName("A 위배 + B 정상 → A retry → A 재실패 시에도 B 정상 적재")
        void partialFailure_doesNotAffectValidDimension() {
            String firstJson = """
                    {
                      "technical_depth":{"score":2,"observation":"english only","evidence_quote":"TPS 10000 을 달성"},
                      "reasoning_communication":{"score":2,"observation":"논리 흐름 명확","evidence_quote":"결제 모듈을 리팩토링"}
                    }
                    """;
            String retryJson = """
                    {
                      "technical_depth":{"score":2,"observation":"still english","evidence_quote":"TPS 10000 을 달성"},
                      "reasoning_communication":{"score":2,"observation":"논리 흐름 명확","evidence_quote":"결제 모듈을 리팩토링"}
                    }
                    """;
            given(aiClient.chat(any()))
                    .willReturn(mockChatResponse("first"))
                    .willReturn(mockChatResponse("retry"));
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth", "reasoning_communication"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth", "reasoning_communication"),
                    USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).containsExactly("reasoning_communication");
            assertThat(result.dimensionScores().get("reasoning_communication").score()).isEqualTo(2);
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(retryFailedCount("technical_depth", "observation")).isEqualTo(1.0);
            assertThat(retryFailedCount("reasoning_communication", "observation")).isZero();
        }
    }

    @Nested
    @DisplayName("score=null 정책 위배")
    class ScoreNullTriggersRetry {

        @Test
        @DisplayName("1차 응답에 score=null 이면 INVALID_SCORE 위배 → retry 진입 → retry 정상 시 적재")
        void scoreNullInFirstResponse_triggersRetry() {
            String firstJson = """
                    {"technical_depth":{"score":null,"observation":"구체적 답변","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            String retryJson = """
                    {"technical_depth":{"score":2,"observation":"구체적 답변","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            given(aiClient.chat(any()))
                    .willReturn(mockChatResponse("first"))
                    .willReturn(mockChatResponse("retry"));
            given(responseParser.extractJson("first")).willReturn(firstJson);
            given(responseParser.extractJson("retry")).willReturn(retryJson);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            verify(aiClient, times(2)).chat(any());
            assertThat(result.scoredDimensions()).containsExactly("technical_depth");
            assertThat(result.dimensionScores().get("technical_depth").score()).isEqualTo(2);
            assertThat(retryFailedCount("technical_depth", "score")).isZero();
        }
    }

    @Nested
    @DisplayName("\"관련 발언 없음\" sentinel 매핑")
    class NotEvaluableSentinel {

        @Test
        @DisplayName("LLM 응답 score=null + observation=\"관련 발언 없음\" → NOT_EVALUABLE 매핑 + retry 없음")
        void notEvaluableSentinel_mapsToNotEvaluableWithoutRetry() {
            String json = """
                    {"technical_depth":{"score":null,"observation":"관련 발언 없음","evidence_quote":"관련 발언 없음"}}
                    """;
            given(aiClient.chat(any())).willReturn(mockChatResponse("first"));
            given(responseParser.extractJson("first")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            verify(aiClient, times(1)).chat(any());
            assertThat(result.scoredDimensions()).isEmpty();
            var ds = result.dimensionScores().get("technical_depth");
            assertThat(ds.score()).isNull();
            assertThat(ds.status()).isEqualTo(DimensionStatus.NOT_EVALUABLE);
            assertThat(ds.observation()).isEqualTo("관련 발언 없음");
            assertThat(retryFailedCount("technical_depth", "score")).isZero();
        }

        @Test
        @DisplayName("A=sentinel + B=정상 → A 는 NOT_EVALUABLE / B 는 OK 적재 + retry 없음")
        void partialSentinel_perDimensionMapping() {
            String json = """
                    {
                      "technical_depth":{"score":null,"observation":"관련 발언 없음","evidence_quote":"관련 발언 없음"},
                      "reasoning_communication":{"score":2,"observation":"논리 흐름 명확","evidence_quote":"결제 모듈을 리팩토링"}
                    }
                    """;
            given(aiClient.chat(any())).willReturn(mockChatResponse("first"));
            given(responseParser.extractJson("first")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth", "reasoning_communication"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth", "reasoning_communication"),
                    USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            verify(aiClient, times(1)).chat(any());
            assertThat(result.scoredDimensions()).containsExactly("reasoning_communication");
            assertThat(result.dimensionScores().get("technical_depth").status())
                    .isEqualTo(DimensionStatus.NOT_EVALUABLE);
            assertThat(result.dimensionScores().get("reasoning_communication").status())
                    .isEqualTo(DimensionStatus.OK);
            assertThat(result.dimensionScores().get("reasoning_communication").score()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("응답 차원 누락 / 파싱 실패")
    class MissingOrFallback {

        @Test
        @DisplayName("응답에 차원 누락 → INVALID_SCORE retry 진입 → 2회 모두 누락 시 retry_failed 로 적재")
        void missingDimension_triggersRetry_thenRetryFailed() {
            String json = "{}";
            given(aiClient.chat(any())).willReturn(mockChatResponse("first"));
            given(responseParser.extractJson("first")).willReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            verify(aiClient, times(2)).chat(any());
            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(result.dimensionScores().get("technical_depth").observation())
                    .startsWith("retry_failed:");
            assertThat(retryFailedCount("technical_depth", "score")).isEqualTo(1.0);
        }

        @Test
        @DisplayName("JSON 파싱 실패 → 전체 fallback")
        void invalidJson_fallback() {
            given(aiClient.chat(any())).willReturn(mockChatResponse("first"));
            given(responseParser.extractJson("first")).willReturn("not-a-json");

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.adapt(aiClient, mockRequest(), rubric,
                    List.of("technical_depth"), USER_ANSWER, INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").observation()).contains("파싱 실패");
        }
    }

    private double retryFailedCount(String dimension, String field) {
        var counter = meterRegistry.find(RubricScoringAdapter.RETRY_FAILED_COUNTER)
                .tag("stage", "verbal")
                .tag("dimension", dimension)
                .tag("field", field)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private ChatResponse mockChatResponse(String content) {
        return new ChatResponse(content, null, "openai", "gpt-4o-mini", false, false);
    }

    private ChatRequest mockRequest() {
        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "test")))
                .callType("rubric_scorer")
                .build();
    }

    private Rubric createRubric(String rubricId, List<String> dimRefs) {
        List<DimensionRef> dims = dimRefs.stream()
                .map(ref -> new DimensionRef(ref, 1.0))
                .toList();
        return new Rubric(rubricId, "테스트", dims, Map.of(), Map.of());
    }
}
