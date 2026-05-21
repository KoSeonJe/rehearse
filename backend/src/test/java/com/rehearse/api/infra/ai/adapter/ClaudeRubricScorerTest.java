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
import com.rehearse.api.infra.ai.client.ClaudeRubricScorerClient;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClaudeRubricScorer — JSON object instruction 주입 + RubricScoringPipeline 위임")
class ClaudeRubricScorerTest {

    private static final Long INTERVIEW_ID = 100L;
    private static final Long QUESTION_ID = 200L;
    private static final String USER_ANSWER = "저는 작년에 결제 모듈을 리팩토링하여 TPS 10000 을 달성했습니다";

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);

    private ClaudeRubricScorerClient client;
    private RubricScorerPromptBuilder promptBuilder;
    private AiResponseParser responseParser;
    private MeterRegistry meterRegistry;
    private ClaudeRubricScorer adapter;

    @BeforeEach
    void setUp() {
        client = mock(ClaudeRubricScorerClient.class);
        promptBuilder = mock(RubricScorerPromptBuilder.class);
        responseParser = mock(AiResponseParser.class);
        meterRegistry = new SimpleMeterRegistry();
        RubricScorerResponseValidator validator = new RubricScorerResponseValidator();
        ObjectMapper objectMapper = new ObjectMapper();
        RubricScoringPipeline executor = new RubricScoringPipeline(
                responseParser, objectMapper, validator, promptBuilder, meterRegistry);
        adapter = new ClaudeRubricScorer(client, promptBuilder, executor);
    }

    @Test
    @DisplayName("system prompt 앞에 JSON object 강제 지시문이 prepend 된 채 client.call 호출")
    void score_prependsJsonObjectInstructionToSystemPrompt() {
        String json = """
                {"technical_depth":{"score":2,"observation":"구체적 수치 인용","evidence_quote":"TPS 10000 을 달성"}}
                """;
        stubPrompt();
        when(client.call(anyString(), anyString())).thenReturn("first-raw");
        when(responseParser.extractJson("first-raw")).thenReturn(json);

        Rubric rubric = createRubric();
        adapter.score(mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS, rubric,
                List.of("technical_depth"), InterviewLevel.JUNIOR, INTERVIEW_ID, QUESTION_ID);

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(client, times(1)).call(systemCaptor.capture(), anyString());
        assertThat(systemCaptor.getValue())
                .startsWith("You MUST respond with a single JSON object only.")
                .contains("sys-content");
    }

    @Test
    @DisplayName("정상 응답 → scoredDimensions 매핑")
    void score_validResponse_persistsDimensions() {
        String json = """
                {"technical_depth":{"score":2,"observation":"구체적 수치 인용","evidence_quote":"TPS 10000 을 달성"}}
                """;
        stubPrompt();
        when(client.call(anyString(), anyString())).thenReturn("first-raw");
        when(responseParser.extractJson("first-raw")).thenReturn(json);

        RubricScoringResult result = adapter.score(
                mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS, createRubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR,
                INTERVIEW_ID, QUESTION_ID);

        assertThat(result.scoredDimensions()).containsExactly("technical_depth");
        assertThat(result.dimensionScores().get("technical_depth").score()).isEqualTo(2);
    }

    @Test
    @DisplayName("JSON 파싱 실패 → 전체 fallback (notApplicable + 파싱 실패 사유)")
    void score_invalidJson_fallback() {
        stubPrompt();
        when(client.call(anyString(), anyString())).thenReturn("first-raw");
        when(responseParser.extractJson("first-raw")).thenReturn("not-a-json");

        RubricScoringResult result = adapter.score(
                mockQuestion(), USER_ANSWER, SAMPLE_ANALYSIS, createRubric(),
                List.of("technical_depth"), InterviewLevel.JUNIOR,
                INTERVIEW_ID, QUESTION_ID);

        assertThat(result.scoredDimensions()).isEmpty();
        assertThat(result.dimensionScores().get("technical_depth").observation())
                .contains("파싱 실패");
    }

    private void stubPrompt() {
        Map<String, Object> schema = Map.of("type", "object");
        when(promptBuilder.build(any(), any(), any(), any(), any(), any()))
                .thenReturn(new RubricScorerPromptBuilder.PromptBundle("sys-content", "usr", schema));
    }

    private Question mockQuestion() {
        return mock(Question.class);
    }

    private Rubric createRubric() {
        return new Rubric("test-v1", "테스트",
                List.of(new DimensionRef("technical_depth", 1.0)),
                Map.of(), Map.of());
    }
}
