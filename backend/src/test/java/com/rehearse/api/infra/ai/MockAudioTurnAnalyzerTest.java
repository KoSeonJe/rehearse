package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.models.service.AudioTurnAnalyzer;
import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.adapter.OpenAiAudioTurnAnalyzer;
import com.rehearse.api.infra.ai.client.OpenAiAudioTurnAnalyzerClient;
import com.rehearse.api.infra.ai.properties.OpenAiAudioTurnAnalyzerProperties;
import com.rehearse.api.infra.ai.config.OpenAiAudioTurnAnalyzerRestClientConfig;
import com.rehearse.api.infra.ai.properties.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.exception.AudioChatFallbackRequiredException;
import com.rehearse.api.infra.ai.prompt.AudioTurnAnalyzerPromptBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("Audio Turn Analyzer 조건부 빈 등록 + Mock 동작 검증 (Claude/Resilient 미지원 — OpenAI 단독)")
class MockAudioTurnAnalyzerTest {

    @Test
    @DisplayName("Mock 단독 호출 시 항상 AudioChatFallbackRequiredException 을 던져 text-only fallback 신호를 보낸다")
    void analyze_alwaysThrowsFallbackSignal() {
        MockAudioTurnAnalyzer mock = new MockAudioTurnAnalyzer();
        MultipartFile audio = new MockMultipartFile("audio", "a.wav", "audio/wav", new byte[]{1});

        assertThatThrownBy(() -> mock.analyze(audio, "Q", ReferenceType.MODEL_ANSWER, QuestionCategory.CONCEPT))
                .isInstanceOf(AudioChatFallbackRequiredException.class);
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        HttpClientAutoConfiguration.class,
                        RestClientAutoConfiguration.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(AiResponseParser.class, () -> new AiResponseParser(new ObjectMapper()))
                .withBean(AudioTurnAnalyzerPromptBuilder.class,
                        () -> org.mockito.Mockito.mock(AudioTurnAnalyzerPromptBuilder.class))
                .withBean(OpenAiCommonProperties.class, () -> new OpenAiCommonProperties("test-key"))
                .withBean(OpenAiAudioTurnAnalyzerProperties.class,
                        () -> new OpenAiAudioTurnAnalyzerProperties(
                                "gpt-4o-mini-audio-preview", 60_000L, 1024, 0.2,
                                "https://api.openai.com/v1/chat/completions"))
                .withUserConfiguration(
                        OpenAiAudioTurnAnalyzerRestClientConfig.class,
                        OpenAiAudioTurnAnalyzerClient.class,
                        OpenAiAudioTurnAnalyzer.class,
                        MockAudioTurnAnalyzer.class);
    }

    @Test
    @DisplayName("openai.api-key 부재 시 Mock 이 빈으로 등록된다")
    void mock_registered_when_openAiKeyAbsent() {
        baseRunner()
                .withPropertyValues("openai.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AudioTurnAnalyzer.class);
                    assertThat(context.getBean(AudioTurnAnalyzer.class))
                            .isInstanceOf(MockAudioTurnAnalyzer.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 OpenAI adapter 가 등록되고 Mock 은 비활성화된다")
    void openAiAdapter_registered_when_openAiKeyPresent() {
        baseRunner()
                .withPropertyValues("openai.api-key=sk-real")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AudioTurnAnalyzer.class);
                    assertThat(context.getBean(AudioTurnAnalyzer.class))
                            .isInstanceOf(OpenAiAudioTurnAnalyzer.class);
                });
    }
}
