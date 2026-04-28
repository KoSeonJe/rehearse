package com.rehearse.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Configuration
@EnableAsync
public class RubricScoringExecutorConfig {

    public static final String RUBRIC_SCORING_EXECUTOR = "rubricScoringExecutor";

    @Bean(RUBRIC_SCORING_EXECUTOR)
    public Executor rubricScoringExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                4,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                Thread.ofVirtual().name("rubric-scorer-", 0).factory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.allowCoreThreadTimeOut(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));

        return executor;
    }
}
