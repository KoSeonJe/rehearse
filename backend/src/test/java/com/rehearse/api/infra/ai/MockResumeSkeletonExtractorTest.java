package com.rehearse.api.infra.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.infra.ai.adapter.OpenAiResumeSkeletonExtractor;
import com.rehearse.api.infra.ai.client.OpenAiResumeExtractorClient;
import com.rehearse.api.infra.ai.config.OpenAiCommonProperties;
import com.rehearse.api.infra.ai.config.OpenAiResumeExtractorRestClientConfig;
import com.rehearse.api.infra.ai.config.OpenAiResumeSkeletonProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayName("Resume Skeleton Extractor 조건부 빈 등록")
class MockResumeSkeletonExtractorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    HttpClientAutoConfiguration.class,
                    RestClientAutoConfiguration.class))
            .withUserConfiguration(TestPromptConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(OpenAiResponsesOutputTextExtractor.class)
            .withBean(AiResponseParser.class, () -> new AiResponseParser(new ObjectMapper()))
            .withBean(OpenAiResumeSkeletonProperties.class,
                    () -> new OpenAiResumeSkeletonProperties(
                            "gpt-4o-mini", 60_000L, 12_000, 0.2, "https://api.openai.com/v1/responses"))
            .withBean(OpenAiCommonProperties.class,
                    () -> new OpenAiCommonProperties("test-key"))
            .withUserConfiguration(
                    OpenAiResumeExtractorRestClientConfig.class,
                    OpenAiResumeExtractorClient.class,
                    OpenAiResumeSkeletonExtractor.class,
                    MockResumeSkeletonExtractor.class);

    @Test
    @DisplayName("openai.api-key 부재 시 MockResumeSkeletonExtractor 가 ResumeSkeletonExtractor 빈으로 등록된다")
    void mockExtractor_registered_when_apiKeyAbsent() {
        contextRunner
                .withPropertyValues("openai.api-key=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ResumeSkeletonExtractor.class);
                    assertThat(context.getBean(ResumeSkeletonExtractor.class))
                            .isInstanceOf(MockResumeSkeletonExtractor.class);
                });
    }

    @Test
    @DisplayName("openai.api-key 존재 시 OpenAiResumeSkeletonExtractor 가 우선 등록되고 Mock 은 비활성화된다")
    void openAiExtractor_registered_when_apiKeyPresent() {
        contextRunner
                .withPropertyValues("openai.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ResumeSkeletonExtractor.class);
                    assertThat(context.getBean(ResumeSkeletonExtractor.class))
                            .isInstanceOf(OpenAiResumeSkeletonExtractor.class);
                });
    }

    @Configuration
    static class TestPromptConfig {
        @Bean
        org.springframework.core.io.support.PathMatchingResourcePatternResolver resolver() {
            return new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        }
    }
}
