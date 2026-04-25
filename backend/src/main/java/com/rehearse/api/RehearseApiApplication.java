package com.rehearse.api;

import com.rehearse.api.global.config.IntentClassifierProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry(order = Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(IntentClassifierProperties.class)
public class RehearseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RehearseApiApplication.class, args);
    }
}
