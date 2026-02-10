package com.scb.backup.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AsyncConfig - Configuration for asynchronous task execution.
 *
 * This configuration enables async processing and defines a custom thread pool
 * for the BackupPollerService to run polling tasks in background threads.
 *
 * @author SCB ePricing Team
 * @version 1.0
 * @since 2026-02-10
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a thread pool executor for backup polling tasks.
     *
     * This executor is used by BackupPollerService to run polling operations
     * asynchronously without blocking the main request thread.
     *
     * Configuration:
     * - Core Pool Size: 5 threads (minimum threads always alive)
     * - Max Pool Size: 10 threads (maximum threads that can be created)
     * - Queue Capacity: 25 tasks (tasks waiting for a thread)
     * - Thread Name Prefix: "BackupPoller-" (for easy identification in logs)
     *
     * @return Executor configured for backup polling tasks
     */
    @Bean(name = "backupPollerExecutor")
    public Executor backupPollerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("BackupPoller-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("Initialized BackupPoller thread pool: corePoolSize=5, maxPoolSize=10, queueCapacity=25");
        return executor;
    }
}

