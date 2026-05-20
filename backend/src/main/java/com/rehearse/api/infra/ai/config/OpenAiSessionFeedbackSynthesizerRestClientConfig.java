package com.rehearse.api.infra.ai.config;

import com.rehearse.api.infra.ai.properties.OpenAiSessionFeedbackSynthesizerProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiSessionFeedbackSynthesizerRestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient openAiSessionFeedbackSynthesizerRestClient(
            RestClient.Builder restClientBuilder,
            OpenAiSessionFeedbackSynthesizerProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(Duration.ofMillis(properties.timeoutMs()));
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
