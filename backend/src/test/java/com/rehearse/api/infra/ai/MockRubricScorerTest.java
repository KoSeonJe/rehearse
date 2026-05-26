package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.infra.ai.adapter.ClaudeRubricScorer;
import com.rehearse.api.infra.ai.adapter.OpenAiRubricScorer;
import com.rehearse.api.infra.ai.adapter.ResilientRubricScorer;
import com.rehearse.api.infra.ai.adapter.RubricScorerResponseValidator;
import com.rehearse.api.infra.ai.adapter.RubricScoringPipeline;
import com.rehearse.api.infra.ai.client.ClaudeRubricScorerClient;
import com.rehearse.api.infra.ai.client.OpenAiRubricScorerClient;
import com.rehearse.api.infra.ai.properties.ClaudeRubricScorerProperties;
import com.rehearse.api.infra.ai.config.ClaudeRubricScorerRestClientConfig;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.properties.OpenAiRubricScorerProperties;
import com.rehearse.api.infra.ai.config.OpenAiRubricScorerRestClientConfig;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import com.rehearse.api.infra.ai.prompt.RubricScorerPromptBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Map;

@DisplayName("RubricScorer 조건부 빈 등록 + Mock 동작 검증")
class MockRubricScorerTest {

    @Test
    @DisplayName("Mock 단독 호출 시 dimensionsToScore 별 score=2 fallback 반환")
    void score_returnsMockScore() {
        MockRubricScorer mock = new MockRubricScorer();
        Rubric rubric = new Rubric("mock-v1", "테스트",
                List.of(new DimensionRef("technical_depth", 1.0)),
                Map.of(), Map.of());

        RubricScoringResult result = mock.score(
                null, null, rubric,
                List.of("technical_depth"), InterviewLevel.JUNIOR, 1L, 2L);

        assertThat(result.rubricId()).isEqualTo("mock-v1");
        assertThat(result.scoredDimensions()).containsExactly("technical_depth");
        assertThat(result.dimensionScores().get("technical_depth").score()).isEqualTo(2);
        assertThat(result.dimensionScores().get("technical_depth").observation()).contains("Mock");
    }

    private ApplicationContextRunner baseRunner() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        HttpClientAutoConfiguration.class,
                        RestClientAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(AiResponseParser.class, () -> new AiResponseParser(new ObjectMapper()))
                .withBean(SimpleMeterRegistry.class, () -> reg)
                .withBean(ContextEngineeringMetrics.class, () -> new ContextEngineeringMetrics(reg))
                .withBean(AiCallMetrics.class, () -> new AiCallMetrics(reg, new ContextEngineeringMetrics(reg)))
                .withBean(RubricScorerResponseValidator.class, RubricScorerResponseValidator::new)
                .withBean(RubricScorerPromptBuilder.class, () -> org.mockito.Mockito.mock(RubricScorerPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiRubricScorerProperties.class,
                        () -> new OpenAiRubricScorerProperties(
                                "gpt-4o-mini", 60_000L, 1536, 0.2, "https://api.openai.com/v1/chat/completions"))
                .withBean(ClaudeRubricScorerProperties.class,
                        () -> new ClaudeRubricScorerProperties(
                                "claude-sonnet-4-20250514", 60_000L, 1536, 0.2, "https://api.anthropic.com/v1/messages"))
                .withUserConfiguration(
                        OpenAiRubricScorerRestClientConfig.class,
                        ClaudeRubricScorerRestClientConfig.class,
                        OpenAiRubricScorerClient.class,
                        ClaudeRubricScorerClient.class,
                        RubricScoringPipeline.class,
                        OpenAiRubricScorer.class,
                        ClaudeRubricScorer.class,
                        ResilientRubricScorer.class,
                        MockRubricScorer.class);
    }

    @Test
    @DisplayName("openai.api-key + claude.api-key 모두 부재 시 Mock 이 빈으로 등록된다")
    void mockScorer_registered_when_bothApiKeysAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RubricScorer.class);
                    assertThat(context.getBean(RubricScorer.class))
                            .isInstanceOf(MockRubricScorer.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=test-key", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RubricScorer.class))
                            .isInstanceOf(ResilientRubricScorer.class);
                    assertThat(context).doesNotHaveBean(MockRubricScorer.class);
                });
    }

    @Test
    @DisplayName("claude.api-key 만 존재해도 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_onlyClaudeKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RubricScorer.class))
                            .isInstanceOf(ResilientRubricScorer.class);
                    assertThat(context).doesNotHaveBean(MockRubricScorer.class);
                });
    }
}
