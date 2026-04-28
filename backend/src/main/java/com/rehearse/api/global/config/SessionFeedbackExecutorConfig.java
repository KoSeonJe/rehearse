package com.rehearse.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class SessionFeedbackExecutorConfig {

    public static final String SESSION_FEEDBACK_EXECUTOR = "sessionFeedbackExecutor";

    @Bean(SESSION_FEEDBACK_EXECUTOR)
    public Executor sessionFeedbackExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("session-feedback-", 0).factory()
        );
    }
}
