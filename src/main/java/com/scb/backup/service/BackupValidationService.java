package com.scb.backup.service;

import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.utils.AppConstants;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * BackupValidationService - Service for validating backup parameters and configurations.
 *
 * This service provides validation logic for batch parameters and YBA backup configurations
 * to ensure all required fields are present before initiating backup operations.
 *
 * <p><b>Validation Rules:</b></p>
 * <ul>
 *   <li>Batch parameters must not be null or empty</li>
 *   <li>Batch ID is required</li>
 *   <li>Category code is required</li>
 *   <li>YBA API token is required</li>
 *   <li>Universe UUID is required</li>
 * </ul>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@Service
@Slf4j
public class BackupValidationService {

    /**
     * Validates batch parameters before backup execution.
     *
     * Ensures that all required batch parameters are present and not empty.
     * This method is called before initiating any backup operation.
     *
     * @param batchParams Map containing batch parameters (batchId, categoryCode)
     * @throws IllegalArgumentException if batch parameters are null, empty, or missing required fields
     */
    public void validateBatchParams(Map<String, Object> batchParams) {
        if (batchParams == null || batchParams.isEmpty()) {
            throw new IllegalArgumentException("Batch parameters cannot be null or empty");
        }

        validateRequiredParam(batchParams, AppConstants.BATCH_ID, "Batch ID");
        validateRequiredParam(batchParams, AppConstants.CATEGORY_CODE, "Category code");
    }

    /**
     * Validates YBA backup configuration.
     *
     * Ensures that the backup configuration contains all required fields for
     * YBA API integration, including API token and universe UUID.
     *
     * @param config YBA dynamic configuration object
     * @param categoryCode Backup category code for error messaging
     * @throws IllegalArgumentException if configuration is null or missing required fields
     */
    void validateBackupConfig(YbaDynamicConfig config, String categoryCode) {
        if (config == null) {
            throw new IllegalArgumentException("No backup configuration found for category: " + categoryCode);
        }

        if (StringUtils.isBlank(config.getApiToken())) {
            throw new IllegalArgumentException("API token is required for backup configuration");
        }

        if (StringUtils.isBlank(config.getUniverseUuid())) {
            throw new IllegalArgumentException("Universe UUID is required for backup configuration");
        }
    }

    /**
     * Validates a required parameter in the batch parameters map.
     *
     * Checks if the parameter exists and is not null or blank (for String values).
     *
     * @param params Map containing parameters to validate
     * @param key Parameter key to check
     * @param description Human-readable description for error messages
     * @throws IllegalArgumentException if parameter is missing, null, or blank
     */
    private void validateRequiredParam(Map<String, Object> params, String key, String description) {
        Object value = params.get(key);
        if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
            throw new IllegalArgumentException(description + " is required");
        }
    }
}
