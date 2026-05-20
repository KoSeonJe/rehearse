package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.adapter.ClaudeResumeQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.OpenAiResumeQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.ResilientResumeQuestionGenerator;
import com.rehearse.api.infra.ai.client.ClaudeResumeQuestionClient;
import com.rehearse.api.infra.ai.client.OpenAiResumeQuestionClient;
import com.rehearse.api.infra.ai.config.ClaudeResumeQuestionProperties;
import com.rehearse.api.infra.ai.config.ClaudeResumeQuestionRestClientConfig;
import com.rehearse.api.infra.ai.config.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.config.OpenAiResumeQuestionProperties;
import com.rehearse.api.infra.ai.config.OpenAiResumeQuestionRestClientConfig;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("Resume Question Generator 조건부 빈 등록 + Mock 동작 검증")
class MockResumeQuestionGeneratorTest {

    @Test
    @DisplayName("Mock 단독 호출 시 opener/main 개수만큼 mock 질문을 반환한다")
    void generate_returnsMockQuestions() {
        MockResumeQuestionGenerator mock = new MockResumeQuestionGenerator();
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "r-1", "hash", CandidateLevel.MID, "backend", List.of());

        GeneratedResumeQuestions result = mock.generate(skeleton, 1, 3);

        assertThat(result.openers()).hasSize(1);
        assertThat(result.mains()).hasSize(3);
        assertThat(result.openers().get(0).question()).contains("[Mock]");
    }

    private ApplicationContextRunner baseRunner() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        HttpClientAutoConfiguration.class,
                        RestClientAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(AiResponseParser.class, () -> new AiResponseParser(new ObjectMapper(), null, null))
                .withBean(SimpleMeterRegistry.class, () -> reg)
                .withBean(ContextEngineeringMetrics.class, () -> new ContextEngineeringMetrics(reg))
                .withBean(AiCallMetrics.class, () -> new AiCallMetrics(reg, new ContextEngineeringMetrics(reg)))
                .withBean(com.rehearse.api.infra.ai.prompt.ResumeQuestionPromptBuilder.class,
                        () -> org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.ResumeQuestionPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiResumeQuestionProperties.class,
                        () -> new OpenAiResumeQuestionProperties(
                                "gpt-4o-mini", 60_000L, 14_800, 0.8, "https://api.openai.com/v1/chat/completions"))
                .withBean(ClaudeResumeQuestionProperties.class,
                        () -> new ClaudeResumeQuestionProperties(
                                "claude-sonnet-4-20250514", 60_000L, 14_800, 0.8, "https://api.anthropic.com/v1/messages"))
                .withUserConfiguration(
                        OpenAiResumeQuestionRestClientConfig.class,
                        ClaudeResumeQuestionRestClientConfig.class,
                        OpenAiResumeQuestionClient.class,
                        ClaudeResumeQuestionClient.class,
                        OpenAiResumeQuestionGenerator.class,
                        ClaudeResumeQuestionGenerator.class,
                        ResilientResumeQuestionGenerator.class,
                        MockResumeQuestionGenerator.class);
    }

    @Test
    @DisplayName("openai.api-key + claude.api-key 모두 부재 시 Mock 이 빈으로 등록된다")
    void mockGenerator_registered_when_bothApiKeysAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ResumeQuestionGenerator.class);
                    assertThat(context.getBean(ResumeQuestionGenerator.class))
                            .isInstanceOf(MockResumeQuestionGenerator.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=test-key", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ResumeQuestionGenerator.class))
                            .isInstanceOf(ResilientResumeQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(MockResumeQuestionGenerator.class);
                });
    }

    @Test
    @DisplayName("claude.api-key 만 존재해도 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_onlyClaudeKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ResumeQuestionGenerator.class))
                            .isInstanceOf(ResilientResumeQuestionGenerator.class);
                    assertThat(context).doesNotHaveBean(MockResumeQuestionGenerator.class);
                });
    }
}
