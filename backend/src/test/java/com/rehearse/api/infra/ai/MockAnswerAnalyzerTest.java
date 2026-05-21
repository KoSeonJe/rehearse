package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.adapter.ClaudeAnswerAnalyzer;
import com.rehearse.api.infra.ai.adapter.OpenAiAnswerAnalyzer;
import com.rehearse.api.infra.ai.adapter.ResilientAnswerAnalyzer;
import com.rehearse.api.infra.ai.client.ClaudeAnswerAnalyzerClient;
import com.rehearse.api.infra.ai.client.OpenAiAnswerAnalyzerClient;
import com.rehearse.api.infra.ai.properties.ClaudeAnswerAnalyzerProperties;
import com.rehearse.api.infra.ai.config.ClaudeAnswerAnalyzerRestClientConfig;
import com.rehearse.api.infra.ai.properties.OpenAiAnswerAnalyzerProperties;
import com.rehearse.api.infra.ai.config.OpenAiAnswerAnalyzerRestClientConfig;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("Answer Analyzer 조건부 빈 등록 + Mock 동작 검증")
class MockAnswerAnalyzerTest {

    @Test
    @DisplayName("Mock 단독 호출 시 SKIP recommendation + 기본 dimension gap 을 반환한다")
    void analyze_returnsMockAnalysis() {
        MockAnswerAnalyzer mock = new MockAnswerAnalyzer();

        GeneratedAnswerAnalysis result = mock.analyze(1L, "Q", ReferenceType.MODEL_ANSWER, "A", false);

        assertThat(result.weakestDimension()).isEqualTo("depth");
        assertThat(result.dimensionGaps()).containsKeys("clarity", "depth", "evidence");
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
                .withBean(com.rehearse.api.infra.ai.prompt.AnswerAnalysisPromptBuilder.class,
                        () -> org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.AnswerAnalysisPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiAnswerAnalyzerProperties.class,
                        () -> new OpenAiAnswerAnalyzerProperties(
                                "gpt-4o-mini", 60_000L, 800, 0.2, "https://api.openai.com/v1/chat/completions"))
                .withBean(ClaudeAnswerAnalyzerProperties.class,
                        () -> new ClaudeAnswerAnalyzerProperties(
                                "claude-sonnet-4-20250514", 60_000L, 800, 0.2, "https://api.anthropic.com/v1/messages"))
                .withUserConfiguration(
                        OpenAiAnswerAnalyzerRestClientConfig.class,
                        ClaudeAnswerAnalyzerRestClientConfig.class,
                        OpenAiAnswerAnalyzerClient.class,
                        ClaudeAnswerAnalyzerClient.class,
                        OpenAiAnswerAnalyzer.class,
                        ClaudeAnswerAnalyzer.class,
                        ResilientAnswerAnalyzer.class,
                        MockAnswerAnalyzer.class);
    }

    @Test
    @DisplayName("openai.api-key + claude.api-key 모두 부재 시 Mock 이 빈으로 등록된다")
    void mockAnalyzer_registered_when_bothApiKeysAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AnswerAnalyzer.class);
                    assertThat(context.getBean(AnswerAnalyzer.class))
                            .isInstanceOf(MockAnswerAnalyzer.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=test-key", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AnswerAnalyzer.class))
                            .isInstanceOf(ResilientAnswerAnalyzer.class);
                    assertThat(context).doesNotHaveBean(MockAnswerAnalyzer.class);
                });
    }

    @Test
    @DisplayName("claude.api-key 만 존재해도 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_onlyClaudeKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AnswerAnalyzer.class))
                            .isInstanceOf(ResilientAnswerAnalyzer.class);
                    assertThat(context).doesNotHaveBean(MockAnswerAnalyzer.class);
                });
    }
}
