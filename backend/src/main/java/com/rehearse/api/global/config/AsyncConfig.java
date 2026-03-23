package com.rehearse.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Virtual Thread 환경: Spring Boot 3.4가 자동으로 VT executor 사용
}
