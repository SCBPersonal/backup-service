package com.scb.backup.config;

import com.scb.backup.model.YbaDynamicConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * YbaProperties - Configuration properties for YugabyteDB Anywhere (YBA) integration.
 *
 * This class binds YBA-related configuration from application.yml to Java objects.
 * It manages database-specific backup configurations and HTTP client settings.
 *
 * <p><b>Configuration Structure:</b></p>
 * <pre>
 * yba:
 *   connection:
 *     timeout: 30000
 *   read:
 *     timeout: 60000
 *   retry:
 *     max-attempts: 3
 *   databases:
 *     HWA_EPR_DB_BACKUP_FULL:
 *       api-token: "token123"
 *       universe-uuid: "uuid-456"
 *       full-backup-url: "https://yba-api/backups"
 *       ...
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see YbaDynamicConfig
 * @see WebClientConfig
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "yba")
@Validated
public class YbaProperties {

    /**
     * Map of database configurations keyed by category code.
     * Each entry contains YBA API details and backup settings for a specific database.
     */
    private Map<String, YbaDynamicConfig> databases;

    /**
     * HTTP connection timeout in milliseconds.
     * Default: 30000ms (30 seconds)
     */
    @Value("${yba.connection.timeout:30000}")
    private int connectionTimeout;

    /**
     * HTTP read timeout in milliseconds.
     * Default: 60000ms (60 seconds)
     */
    @Value("${yba.read.timeout:60000}")
    private int readTimeout;

    /**
     * Maximum number of retry attempts for failed HTTP requests.
     * Default: 3 attempts
     */
    @Value("${yba.retry.max-attempts:3}")
    private int maxRetryAttempts;
}
