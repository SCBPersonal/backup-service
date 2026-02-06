package com.scb.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * BackupPollerProperties - Configuration properties for backup job polling.
 *
 * This class holds configuration for the background poller that monitors
 * full backup job completion in YBA. The poller runs in parallel threads
 * to check job status and update the database when backups complete.
 *
 * <p><b>Configuration Example:</b></p>
 * <pre>
 * backup:
 *   poller:
 *     enabled: true
 *     initial-delay-ms: 5000
 *     polling-interval-ms: 30000
 *     max-poll-attempts: 120
 *     thread-pool-size: 5
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-05
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "backup.poller")
public class BackupPollerProperties {

    /**
     * Enable or disable the backup poller.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Initial delay before starting polling (in milliseconds).
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
     * Size of the thread pool for parallel polling.
     * Default: 5 threads
     */
    private int threadPoolSize = 5;

    /**
     * YBA API URL template for checking job status.
     * Placeholders: {customerUuid}, {taskUuid}
     */
    private String jobCompletionCheckUrl = "/api/v1/customers/{customerUuid}/tasks/{taskUuid}";
}

