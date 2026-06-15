package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.models.service.StandardQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.ClaudeStandardQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.OpenAiStandardQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.ResilientStandardQuestionGenerator;
import com.rehearse.api.infra.ai.client.ClaudeQuestionGeneratorClient;
import com.rehearse.api.infra.ai.client.OpenAiQuestionGeneratorClient;
import com.rehearse.api.infra.ai.properties.ClaudeQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.config.ClaudeQuestionGeneratorRestClientConfig;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.properties.OpenAiQuestionGeneratorProperties;
import com.rehearse.api.infra.ai.config.OpenAiQuestionGeneratorRestClientConfig;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("Standard Question Generator 조건부 빈 등록 + Mock 동작 검증")
class MockStandardQuestionGeneratorTest {

    @Test
    @DisplayName("Mock 단독 호출 시 5개 mock 질문을 반환한다")
    void generate_returnsMockQuestions() {
        MockStandardQuestionGenerator mock = new MockStandardQuestionGenerator(new ObjectMapper());

        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL), Set.of(), null, null);

        List<GeneratedQuestion> result = mock.generate(req);

        assertThat(result).hasSize(5);
        assertThat(result.get(0).content()).contains("[Mock]");
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
                .withBean(com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder.class,
                        () -> org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiQuestionGeneratorProperties.class,
                        () -> new OpenAiQuestionGeneratorProperties(
                                "gpt-4o-mini", 60_000L, 8192, 0.9, "https://api.openai.com/v1/chat/completions"))
                .withBean(ClaudeQuestionGeneratorProperties.class,
                        () -> new ClaudeQuestionGeneratorProperties(
                                "claude-sonnet-4-20250514", 60_000L, 8192, 0.9, "https://api.anthropic.com/v1/messages"))
                .withUserConfiguration(
                        OpenAiQuestionGeneratorRestClientConfig.class,
                        ClaudeQuestionGeneratorRestClientConfig.class,
                        OpenAiQuestionGeneratorClient.class,
                        ClaudeQuestionGeneratorClient.class,
                        OpenAiStandardQuestionGenerator.class,
                        ClaudeStandardQuestionGenerator.class,
                        ResilientStandardQuestionGenerator.class,
                        MockStandardQuestionGenerator.class);
    }

    @Test
    @DisplayName("openai.api-key + claude.api-key 모두 부재 시 Mock 이 빈으로 등록된다")
    void mockGenerator_registered_when_bothApiKeysAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(StandardQuestionGenerator.class);
                    assertThat(context.getBean(StandardQuestionGenerator.class))
                            .isInstanceOf(MockStandardQuestionGenerator.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=test-key", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StandardQuestionGenerator.class))
                            .isInstanceOf(ResilientStandardQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(MockStandardQuestionGenerator.class);
                });
    }

    @Test
    @DisplayName("claude.api-key 만 존재해도 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_onlyClaudeKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StandardQuestionGenerator.class))
                            .isInstanceOf(ResilientStandardQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(MockStandardQuestionGenerator.class);
                });
    }
}
