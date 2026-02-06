package com.scb.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * BackupPollerProperties - Configuration properties for backup job polling.
 *
 * This class binds polling-related configuration from application.yml to Java objects.
 * It controls the behavior of the async backup job status poller.
 *
 * <p><b>Configuration Structure:</b></p>
 * <pre>
 * backup:
 *   poller:
 *     enabled: true
 *     initial-delay-ms: 5000
 *     polling-interval-ms: 30000
 *     max-poll-attempts: 120
 *     thread-pool-size: 5
 *     job-completion-check-url: /api/v1/customers/{customerUuid}/tasks/{taskUuid}
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-05
 * @see BackupPollerService
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "backup.poller")
@Validated
public class BackupPollerProperties {

    /**
     * Enable or disable the backup poller.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Initial delay before starting the first poll (in milliseconds).
     * Default: 5000ms (5 seconds)
     */
    private long initialDelayMs = 5000;

    /**
     * Interval between polling attempts (in milliseconds).
     * Default: 30000ms (30 seconds)
     */
    private long pollingIntervalMs = 30000;

    /**
     * Maximum number of polling attempts before giving up.
     * Default: 120 attempts (1 hour with 30s interval)
     */
    private int maxPollAttempts = 120;

    /**
     * Thread pool size for concurrent polling operations.
     * Default: 5 threads
     */
    private int threadPoolSize = 5;

    /**
     * YBA API endpoint template for checking job completion status.
     * Placeholders: {customerUuid}, {taskUuid}
     * Default: /api/v1/customers/{customerUuid}/tasks/{taskUuid}
     */
    private String jobCompletionCheckUrl = "/api/v1/customers/{customerUuid}/tasks/{taskUuid}";
}

