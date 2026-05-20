package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiRubricScorerClient;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiRubricScorer — RubricScoringPipeline 위임 + validate / retry / merge / fallback 시나리오")
class OpenAiRubricScorerTest {

    private static final Long INTERVIEW_ID = 100L;
    private static final Long QUESTION_ID = 200L;
    private static final String USER_ANSWER = "저는 작년에 결제 모듈을 리팩토링하여 TPS 10000 을 달성했습니다";

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);

    private OpenAiRubricScorerClient client;
    private RubricScorerPromptBuilder promptBuilder;
    private AiResponseParser responseParser;
    private MeterRegistry meterRegistry;
    private OpenAiRubricScorer adapter;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiRubricScorerClient.class);
        promptBuilder = mock(RubricScorerPromptBuilder.class);
        responseParser = mock(AiResponseParser.class);
        meterRegistry = new SimpleMeterRegistry();
        RubricScorerResponseValidator validator = new RubricScorerResponseValidator();
        ObjectMapper objectMapper = new ObjectMapper();
        RubricScoringPipeline executor = new RubricScoringPipeline(
                responseParser, objectMapper, validator, promptBuilder, meterRegistry);
        adapter = new OpenAiRubricScorer(client, promptBuilder, executor);
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
            stubPrompt(List.of("technical_depth"));
            when(client.call(eq("sys"), eq("usr"), any())).thenReturn("first-raw");
            when(responseParser.extractJson("first-raw")).thenReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS,
                    rubric, List.of("technical_depth"), InterviewLevel.JUNIOR,
                    INTERVIEW_ID, QUESTION_ID);

            verify(client, times(1)).call(anyString(), anyString(), any());
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
            stubPrompt(List.of("technical_depth"));
            when(client.call(anyString(), anyString(), any()))
                    .thenReturn("first-raw")
                    .thenReturn("retry-raw");
            when(responseParser.extractJson("first-raw")).thenReturn(firstJson);
            when(responseParser.extractJson("retry-raw")).thenReturn(retryJson);
            when(promptBuilder.buildRetryHint(any(), any(), any())).thenReturn("retry-hint");

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS,
                    rubric, List.of("technical_depth"), InterviewLevel.JUNIOR,
                    INTERVIEW_ID, QUESTION_ID);

            verify(client, times(2)).call(anyString(), anyString(), any());
            assertThat(result.scoredDimensions()).containsExactly("technical_depth");
            assertThat(result.dimensionScores().get("technical_depth").observation())
                    .isEqualTo("구체적 수치 인용");
            assertThat(retryFailedCount("technical_depth", "observation")).isZero();
        }
    }

    @Nested
    @DisplayName("1차/2차 모두 위배")
    class RetryFails {

        @Test
        @DisplayName("evidence_quote 가 두 번 모두 userAnswer 미포함 → 해당 dimension omit + 메트릭 +1")
        void evidenceNotInAnswer_retryFails_omits() {
            String firstJson = """
                    {"technical_depth":{"score":2,"observation":"구체적 답변","evidence_quote":"팀에서 했어요 수준"}}
                    """;
            String retryJson = """
                    {"technical_depth":{"score":2,"observation":"구체적 답변","evidence_quote":"루브릭 정의문"}}
                    """;
            stubPrompt(List.of("technical_depth"));
            when(client.call(anyString(), anyString(), any()))
                    .thenReturn("first-raw")
                    .thenReturn("retry-raw");
            when(responseParser.extractJson("first-raw")).thenReturn(firstJson);
            when(responseParser.extractJson("retry-raw")).thenReturn(retryJson);
            when(promptBuilder.buildRetryHint(any(), any(), any())).thenReturn("retry-hint");

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS,
                    rubric, List.of("technical_depth"), InterviewLevel.JUNIOR,
                    INTERVIEW_ID, QUESTION_ID);

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
        @DisplayName("retry 응답에서 위배 dimension 누락 → field=score 메트릭 +1")
        void retryResponseMissingDimension_recordsScoreField() {
            String firstJson = """
                    {"technical_depth":{"score":2,"observation":"english only","evidence_quote":"TPS 10000 을 달성"}}
                    """;
            String retryJson = "{}";
            stubPrompt(List.of("technical_depth"));
            when(client.call(anyString(), anyString(), any()))
                    .thenReturn("first-raw")
                    .thenReturn("retry-raw");
            when(responseParser.extractJson("first-raw")).thenReturn(firstJson);
            when(responseParser.extractJson("retry-raw")).thenReturn(retryJson);
            when(promptBuilder.buildRetryHint(any(), any(), any())).thenReturn("retry-hint");

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS,
                    rubric, List.of("technical_depth"), InterviewLevel.JUNIOR,
                    INTERVIEW_ID, QUESTION_ID);

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
        @DisplayName("A 위배 + B 정상 → A retry → A 재실패해도 B 는 정상 적재")
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
            stubPrompt(List.of("technical_depth", "reasoning_communication"));
            when(client.call(anyString(), anyString(), any()))
                    .thenReturn("first-raw")
                    .thenReturn("retry-raw");
            when(responseParser.extractJson("first-raw")).thenReturn(firstJson);
            when(responseParser.extractJson("retry-raw")).thenReturn(retryJson);
            when(promptBuilder.buildRetryHint(any(), any(), any())).thenReturn("retry-hint");

            Rubric rubric = createRubric("test-v1", List.of("technical_depth", "reasoning_communication"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS, rubric,
                    List.of("technical_depth", "reasoning_communication"),
                    InterviewLevel.JUNIOR, INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).containsExactly("reasoning_communication");
            assertThat(result.dimensionScores().get("reasoning_communication").score()).isEqualTo(2);
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(retryFailedCount("technical_depth", "observation")).isEqualTo(1.0);
            assertThat(retryFailedCount("reasoning_communication", "observation")).isZero();
        }
    }

    @Nested
    @DisplayName("응답 차원 누락 / 파싱 실패")
    class MissingOrFallback {

        @Test
        @DisplayName("응답에 차원 누락 → notApplicable 로 채우고 scoredDimensions 미포함")
        void missingDimension_fillsAsNotApplicable() {
            String json = "{}";
            stubPrompt(List.of("technical_depth"));
            when(client.call(anyString(), anyString(), any())).thenReturn("first-raw");
            when(responseParser.extractJson("first-raw")).thenReturn(json);

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS, rubric,
                    List.of("technical_depth"), InterviewLevel.JUNIOR,
                    INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").score()).isNull();
            assertThat(result.dimensionScores().get("technical_depth").observation())
                    .contains("LLM 응답에 차원 없음");
        }

        @Test
        @DisplayName("JSON 파싱 실패 → 전체 fallback")
        void invalidJson_fallback() {
            stubPrompt(List.of("technical_depth"));
            when(client.call(anyString(), anyString(), any())).thenReturn("first-raw");
            when(responseParser.extractJson("first-raw")).thenReturn("not-a-json");

            Rubric rubric = createRubric("test-v1", List.of("technical_depth"));
            RubricScoringResult result = adapter.score(
                    mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS, rubric,
                    List.of("technical_depth"), InterviewLevel.JUNIOR,
                    INTERVIEW_ID, QUESTION_ID);

            assertThat(result.scoredDimensions()).isEmpty();
            assertThat(result.dimensionScores().get("technical_depth").observation())
                    .contains("파싱 실패");
        }
    }

    private void stubPrompt(List<String> dimensions) {
        Map<String, Object> schema = Map.of("type", "object");
        when(promptBuilder.build(any(), any(), any(), any(), any(), any()))
                .thenReturn(new RubricScorerPromptBuilder.PromptBundle("sys", "usr", schema));
    }

    private double retryFailedCount(String dimension, String field) {
        var counter = meterRegistry.find(RubricScoringPipeline.RETRY_FAILED_COUNTER)
                .tag("stage", "verbal")
                .tag("dimension", dimension)
                .tag("field", field)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private Question mockQuestion() {
        return mock(Question.class);
    }

    private Rubric createRubric(String rubricId, List<String> dimRefs) {
        List<DimensionRef> dims = dimRefs.stream()
                .map(ref -> new DimensionRef(ref, 1.0))
                .toList();
        return new Rubric(rubricId, "테스트", dims, Map.of(), Map.of());
    }
}
