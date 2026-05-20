package com.rehearse.api.infra.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnExpression("!'${claude.api-key:}'.isEmpty()")
public class ClaudeAnswerAnalyzerRestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient claudeAnswerAnalyzerRestClient(
            RestClient.Builder restClientBuilder,
            ClaudeAnswerAnalyzerProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(Duration.ofMillis(properties.timeoutMs()));
        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
