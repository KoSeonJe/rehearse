package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.models.service.FollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.ClaudeFollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.OpenAiFollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.ResilientFollowUpQuestionGenerator;
import com.rehearse.api.infra.ai.client.ClaudeFollowUpQuestionGeneratorClient;
import com.rehearse.api.infra.ai.client.OpenAiFollowUpQuestionGeneratorClient;
import com.rehearse.api.infra.ai.config.ClaudeFollowUpQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.config.ClaudeFollowUpQuestionGeneratorRestClientConfig;
import com.rehearse.api.infra.ai.config.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.config.OpenAiFollowUpQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.config.OpenAiFollowUpQuestionGeneratorRestClientConfig;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
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

@DisplayName("FollowUp Question Generator 조건부 빈 등록 + Mock 동작 검증")
class MockFollowUpQuestionGeneratorTest {

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of("depth", 1), "depth", List.of(), RecommendedNextAction.DEEP_DIVE);

    @Test
    @DisplayName("Mock 단독 호출 시 기본 follow-up 질문 + answerText override 가 적용된다")
    void generate_returnsMockFollowUp() {
        MockFollowUpQuestionGenerator mock = new MockFollowUpQuestionGenerator();

        GeneratedFollowUp result = mock.generate("Q", "원문 답변", SAMPLE_ANALYSIS, null);

        assertThat(result.isSkipped()).isFalse();
        assertThat(result.question()).isNotBlank();
        assertThat(result.answerText()).isEqualTo("원문 답변");
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
                .withBean(com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder.class,
                        () -> org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.FollowUpQuestionPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiFollowUpQuestionGeneratorProperties.class,
                        () -> new OpenAiFollowUpQuestionGeneratorProperties(
                                "gpt-4o-mini", 60_000L, 1024, 0.6, "https://api.openai.com/v1/chat/completions"))
                .withBean(ClaudeFollowUpQuestionGeneratorProperties.class,
                        () -> new ClaudeFollowUpQuestionGeneratorProperties(
                                "claude-sonnet-4-20250514", 60_000L, 1024, 0.6, "https://api.anthropic.com/v1/messages"))
                .withUserConfiguration(
                        OpenAiFollowUpQuestionGeneratorRestClientConfig.class,
                        ClaudeFollowUpQuestionGeneratorRestClientConfig.class,
                        OpenAiFollowUpQuestionGeneratorClient.class,
                        ClaudeFollowUpQuestionGeneratorClient.class,
                        OpenAiFollowUpQuestionGenerator.class,
                        ClaudeFollowUpQuestionGenerator.class,
                        ResilientFollowUpQuestionGenerator.class,
                        MockFollowUpQuestionGenerator.class);
    }

    @Test
    @DisplayName("openai.api-key + claude.api-key 모두 부재 시 Mock 이 빈으로 등록된다")
    void mockGenerator_registered_when_bothApiKeysAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FollowUpQuestionGenerator.class);
                    assertThat(context.getBean(FollowUpQuestionGenerator.class))
                            .isInstanceOf(MockFollowUpQuestionGenerator.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=test-key", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(FollowUpQuestionGenerator.class))
                            .isInstanceOf(ResilientFollowUpQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(MockFollowUpQuestionGenerator.class);
                });
    }

    @Test
    @DisplayName("claude.api-key 만 존재해도 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_onlyClaudeKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(FollowUpQuestionGenerator.class))
                            .isInstanceOf(ResilientFollowUpQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(MockFollowUpQuestionGenerator.class);
                });
    }
}
