package com.rehearse.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String VT_EXECUTOR = "vtExecutor";

    @Bean(VT_EXECUTOR)
    public Executor vtExecutor() {
        Executor delegate = Executors.newVirtualThreadPerTaskExecutor();
        return new DelegatingSecurityContextExecutor(delegate);
    }
}
