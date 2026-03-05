package com.scb.backup.model;

import com.scb.backup.config.YbaProperties;
import com.scb.backup.service.YbaConfigService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * YbaDynamicConfig - Configuration model for YugabyteDB Anywhere (YBA) backup settings.
 *
 * This class represents the configuration for a specific database backup category.
 * It contains all necessary information to interact with the YBA API including
 * authentication tokens, API endpoints, and backup parameters.
 *
 * <p>Instances of this class are created from application.yml configuration and
 * managed by {@link YbaConfigService}.</p>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see YbaProperties
 * @see YbaConfigService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YbaDynamicConfig {
    /** YBA API authentication token */
    private String apiToken;

    /** UUID of the YugabyteDB universe to backup */
    private String universeUuid;

    /** UUID of the YBA customer account */
    private String customerUuid;

    /** UUID of the storage configuration for backup destination */
    private String storageConfigUuid;

    /** YBA API endpoint URL for full backups */
    private String fullBackupUrl;

    /** YBA API endpoint URL for incremental backups */
    private String incrementalBackupUrl;

    /** YBA API endpoint URL to fetch last backup details */
    private String lastBackupUrl;

    /** YBA API endpoint URL template for checking job completion status (contains {taskUuid} placeholder) */
    private String jobCompletionCheckUrl;

    /** Backup expiry time in milliseconds */
    private Long expiryMs;

    /** Type of backup (e.g., "PGSQL_TABLE_TYPE") */
    private String backupType;

    /** Database name to backup */
    private String dbName;

    /** Backup category type (FULL or INCREMENTAL) */
    private String backupCategoryType;

    /** Parent category code for incremental backups */
    private String parentCategory;
}
