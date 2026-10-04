package com.xin.musictag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchExecutorConfiguration {
    @Bean
    public ThreadPoolTaskExecutor batchExecutor(@Value("${MUSIC_BATCH_CONCURRENCY:2}") int configuredConcurrency) {
        int concurrency = Math.max(1, Math.min(configuredConcurrency, 4));
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setThreadNamePrefix("batch-import-");
        executor.initialize();
        return executor;
    }
}
