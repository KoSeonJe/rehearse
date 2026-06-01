package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.models.service.SessionFeedbackSynthesizer;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.infra.ai.adapter.ClaudeSessionFeedbackSynthesizer;
import com.rehearse.api.infra.ai.adapter.OpenAiSessionFeedbackSynthesizer;
import com.rehearse.api.infra.ai.adapter.ResilientSessionFeedbackSynthesizer;
import com.rehearse.api.infra.ai.client.ClaudeSessionFeedbackSynthesizerClient;
import com.rehearse.api.infra.ai.client.OpenAiSessionFeedbackSynthesizerClient;
import com.rehearse.api.infra.ai.properties.ClaudeSessionFeedbackSynthesizerProperties;
import com.rehearse.api.infra.ai.config.ClaudeSessionFeedbackSynthesizerRestClientConfig;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.properties.OpenAiSessionFeedbackSynthesizerProperties;
import com.rehearse.api.infra.ai.config.OpenAiSessionFeedbackSynthesizerRestClientConfig;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Collections;
import java.util.List;

@DisplayName("Session Feedback Synthesizer 조건부 빈 등록 + Mock 동작 검증")
class MockSessionFeedbackSynthesizerTest {

    @Test
    @DisplayName("Mock 단독 호출 시 기본 세션 피드백 + parser 검증을 통과한다")
    void synthesize_returnsMockFeedback() {
        MockSessionFeedbackSynthesizer mock = new MockSessionFeedbackSynthesizer();

        GeneratedSessionFeedback result = mock.synthesize(sampleInput());

        assertThat(result.overall()).isNotNull();
        assertThat(result.strengths()).isNotEmpty();
        assertThat(result.gaps()).isNotEmpty();
        assertThat(result.weekPlan()).isNotEmpty();
    }

    private ApplicationContextRunner baseRunner() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        HttpClientAutoConfiguration.class,
                        RestClientAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(SessionFeedbackParser.class, () -> new SessionFeedbackParser(new ObjectMapper()))
                .withBean(SimpleMeterRegistry.class, () -> reg)
                .withBean(ContextEngineeringMetrics.class, () -> new ContextEngineeringMetrics(reg))
                .withBean(AiCallMetrics.class, () -> new AiCallMetrics(reg, new ContextEngineeringMetrics(reg)))
                .withBean(com.rehearse.api.infra.ai.prompt.SessionFeedbackSynthesizerPromptBuilder.class,
                        () -> org.mockito.Mockito.mock(com.rehearse.api.infra.ai.prompt.SessionFeedbackSynthesizerPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiSessionFeedbackSynthesizerProperties.class,
                        () -> new OpenAiSessionFeedbackSynthesizerProperties(
                                "gpt-4o-mini", 60_000L, 2048, 0.3, "https://api.openai.com/v1/chat/completions"))
                .withBean(ClaudeSessionFeedbackSynthesizerProperties.class,
                        () -> new ClaudeSessionFeedbackSynthesizerProperties(
                                "claude-sonnet-4-20250514", 60_000L, 2048, 0.3, "https://api.anthropic.com/v1/messages"))
                .withUserConfiguration(
                        OpenAiSessionFeedbackSynthesizerRestClientConfig.class,
                        ClaudeSessionFeedbackSynthesizerRestClientConfig.class,
                        OpenAiSessionFeedbackSynthesizerClient.class,
                        ClaudeSessionFeedbackSynthesizerClient.class,
                        OpenAiSessionFeedbackSynthesizer.class,
                        ClaudeSessionFeedbackSynthesizer.class,
                        ResilientSessionFeedbackSynthesizer.class,
                        MockSessionFeedbackSynthesizer.class);
    }

    @Test
    @DisplayName("openai.api-key + claude.api-key 모두 부재 시 Mock 이 빈으로 등록된다")
    void mockSynthesizer_registered_when_bothApiKeysAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SessionFeedbackSynthesizer.class);
                    assertThat(context.getBean(SessionFeedbackSynthesizer.class))
                            .isInstanceOf(MockSessionFeedbackSynthesizer.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=test-key", "claude.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(SessionFeedbackSynthesizer.class))
                            .isInstanceOf(ResilientSessionFeedbackSynthesizer.class);
                    assertThat(context).doesNotHaveBean(MockSessionFeedbackSynthesizer.class);
                });
    }

    @Test
    @DisplayName("claude.api-key 만 존재해도 Resilient 가 Primary 로 등록되고 Mock 은 비활성화된다")
    void resilient_registered_when_onlyClaudeKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=", "claude.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(SessionFeedbackSynthesizer.class))
                            .isInstanceOf(ResilientSessionFeedbackSynthesizer.class);
                    assertThat(context).doesNotHaveBean(MockSessionFeedbackSynthesizer.class);
                });
    }

    private SessionFeedbackInput sampleInput() {
        return new SessionFeedbackInput(
                new SessionFeedbackInput.SessionMetadata(
                        1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 2, 30),
                Collections.emptyList(),
                null, null,
                "all turns scored",
                InterviewLevel.MID
        );
    }
}
