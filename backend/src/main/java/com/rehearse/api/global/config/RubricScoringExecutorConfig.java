package com.rehearse.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class RubricScoringExecutorConfig {

    public static final String RUBRIC_SCORING_EXECUTOR = "rubricScoringExecutor";

    @Bean(RUBRIC_SCORING_EXECUTOR)
    public Executor rubricScoringExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("rubric-scorer-", 0).factory()
        );
    }
}
