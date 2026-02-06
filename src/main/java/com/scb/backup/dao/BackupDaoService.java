package com.scb.backup.dao;

import com.scb.backup.exception.DbBackupException;
import com.scb.backup.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * BackupDaoService - Data Access Object for backup operations.
 *
 * This service handles all database operations related to backup tracking,
 * including backup status management and base backup UUID storage for
 * monthly incremental backups.
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BackupDaoService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    // Full Backup Table Queries
    @Value("${data.insert-full-backup.query}")
    String insertFullBackup;

    @Value("${data.update-full-backup-status.query}")
    String updateFullBackupStatusQuery;

    @Value("${data.update-full-backup-with-base-uuid.query}")
    String updateFullBackupWithBaseUuidQuery;

    @Value("${data.get-base-backup-uuid-from-full-tracker.query}")
    String getBaseBackupUuidFromFullTracker;

    // Incremental Backup Table Queries
    @Value("${data.insert-incremental-backup.query}")
    String insertIncrementalBackup;

    @Value("${data.update-incremental-backup-status.query}")
    String updateIncrementalBackupStatusQuery;

    // Legacy queries for backward compatibility (old batch_db_schedule_event_tracker table)
    @Value("${data.db-schedule-backup-insert.query:#{null}}")
    String insertScheduleBackup;

    @Value("${data.update-schedule-backup.query:#{null}}")
    String updateDbackupStatus;

    @Value("${data.insert-base-backup-uuid.query:#{null}}")
    String insertBaseBackupUuid;

    // ==================== FULL BACKUP TABLE OPERATIONS ====================

    /**
     * Inserts a new full backup record into the full_backup_tracker table.
     *
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param businessDate Business date
     * @param backupMonth Month in YYYY-MM format
     * @param taskUuid YBA task UUID for polling
     * @throws DbBackupException if database operation fails
     */
    public void insertFullBackupRecord(String batchId, String categoryCode, Date businessDate,
                                       String backupMonth, String taskUuid) {
        log.info("Inserting full backup record for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batchId", batchId);
            param.put("categoryCode", categoryCode);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("backupMonth", backupMonth);
            param.put("backupStatus", AppConstants.BACKUP_INPROGRESS_STATUS);
            param.put("taskUuid", taskUuid);

            jdbcTemplate.update(insertFullBackup, param);
            log.info("Full backup record inserted successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Unable to insert full backup record for category: {}", categoryCode, e);
            throw new DbBackupException("Error inserting full backup record for category: " + categoryCode, e);
        }
    }

    /**
     * Updates the full backup status with error message.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param status Backup status (SUCCESS, FAILED)
     * @param errorMessage Error message (can be null for success)
     * @throws DbBackupException if database operation fails
     */
    public void updateFullBackupStatus(String categoryCode, String backupMonth, String status, String errorMessage) {
        log.info("Updating full backup status for category: {}, month: {}, status: {}", categoryCode, backupMonth, status);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);
            param.put("backupStatus", status);
            param.put("errorMessage", errorMessage);

            jdbcTemplate.update(updateFullBackupStatusQuery, param);
            log.info("Full backup status updated successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Unable to update full backup status for category: {}", categoryCode, e);
            throw new DbBackupException("Error updating full backup status for category: " + categoryCode, e);
        }
    }

    /**
     * Updates the full backup record with base backup UUID and marks it as SUCCESS.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param baseBackupUuid Base backup UUID from YBA
     * @throws DbBackupException if database operation fails
     */
    public void updateFullBackupWithBaseUuid(String categoryCode, String backupMonth, String baseBackupUuid) {
        log.info("Updating full backup with base UUID for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);
            param.put("baseBackupUuid", baseBackupUuid);

            jdbcTemplate.update(updateFullBackupWithBaseUuidQuery, param);
            log.info("Full backup updated with base UUID successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Unable to update full backup with base UUID for category: {}", categoryCode, e);
            throw new DbBackupException("Error updating full backup with base UUID for category: " + categoryCode, e);
        }
    }

    /**
     * Retrieves the base backup UUID from full_backup_tracker for a given category and month.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @return Base backup UUID if found, null otherwise
     * @throws DbBackupException if database query fails
     */
    public String getBaseBackupUuidFromDb(String categoryCode, String backupMonth) {
        log.info("Retrieving base backup UUID for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);

            String baseUuid = jdbcTemplate.queryForObject(getBaseBackupUuidFromFullTracker, param, String.class);
            log.info("Base backup UUID retrieved: {}", baseUuid);
            return baseUuid;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("No base backup UUID found for category: {}, month: {}", categoryCode, backupMonth);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving base backup UUID for category: {}, month: {}", categoryCode, backupMonth, e);
            throw new DbBackupException("Error retrieving base backup UUID for category: " + categoryCode, e);
        }
    }

    // ==================== INCREMENTAL BACKUP TABLE OPERATIONS ====================

    /**
     * Inserts a new incremental backup record into the incremental_backup_tracker table.
     *
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param businessDate Business date
     * @param backupMonth Month in YYYY-MM format
     * @param baseBackupUuid Base backup UUID reference
     * @param taskUuid YBA task UUID
     * @throws DbBackupException if database operation fails
     */
    public void insertIncrementalBackupRecord(String batchId, String categoryCode, Date businessDate,
                                              String backupMonth, String baseBackupUuid, String taskUuid) {
        log.info("Inserting incremental backup record for category: {}, base UUID: {}", categoryCode, baseBackupUuid);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batchId", batchId);
            param.put("categoryCode", categoryCode);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("backupMonth", backupMonth);
            param.put("baseBackupUuid", baseBackupUuid);
            param.put("backupStatus", AppConstants.BACKUP_INPROGRESS_STATUS);
            param.put("taskUuid", taskUuid);

            jdbcTemplate.update(insertIncrementalBackup, param);
            log.info("Incremental backup record inserted successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Unable to insert incremental backup record for category: {}", categoryCode, e);
            throw new DbBackupException("Error inserting incremental backup record for category: " + categoryCode, e);
        }
    }

    /**
     * Updates the incremental backup status.
     *
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param status Backup status (SUCCESS, FAILED)
     * @param backupResponse YBA API response (can be null)
     * @param errorMessage Error message (can be null for success)
     * @throws DbBackupException if database operation fails
     */
    public void updateIncrementalBackupStatus(String batchId, String categoryCode, String status,
                                              String backupResponse, String errorMessage) {
        log.info("Updating incremental backup status for batch: {}, category: {}, status: {}", batchId, categoryCode, status);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batchId", batchId);
            param.put("categoryCode", categoryCode);
            param.put("backupStatus", status);
            param.put("backupResponse", backupResponse);
            param.put("errorMessage", errorMessage);

            jdbcTemplate.update(updateIncrementalBackupStatusQuery, param);
            log.info("Incremental backup status updated successfully for batch: {}", batchId);
        } catch (Exception e) {
            log.error("Unable to update incremental backup status for batch: {}", batchId, e);
            throw new DbBackupException("Error updating incremental backup status for batch: " + batchId, e);
        }
    }

    // ==================== LEGACY METHODS (For backward compatibility) ====================

    /**
     * Legacy method for inserting backup details into batch_db_schedule_event_tracker table.
     * This method is kept for backward compatibility with existing code.
     *
     * @deprecated Use insertFullBackupRecord or insertIncrementalBackupRecord instead
     */
    @Deprecated
    public void insertBackupDetails(Map<String,Object> backupDetails, String backupStatus, String backupType) {
        if (insertScheduleBackup == null) {
            log.warn("Legacy insertScheduleBackup query not configured. Skipping legacy insert.");
            return;
        }
        log.info("Inserting data in legacy Backup Table:");
        try {
            backupDetails.put("batch_id", backupDetails.get(AppConstants.BATCH_ID));
            backupDetails.put("batchCategory", backupDetails.get(AppConstants.CATEGORY_CODE));
            backupDetails.put("backupStatus", AppConstants.BACKUP_INPROGRESS_STATUS);
            backupDetails.put("backupType", backupType);
            backupDetails.put("business_date", AppConstants.BUSINESS_DATE);
            backupDetails.put("start_time", Timestamp.valueOf(LocalDateTime.now()));
            jdbcTemplate.update(insertScheduleBackup, backupDetails);
        } catch (Exception e) {
            log.error("Unable to Insert data in legacy Backup Table ", e);
            throw new DbBackupException("Unable to Insert data in legacy Backup Table", e);
        }
    }

    /**
     * Legacy method for updating backup status in batch_db_schedule_event_tracker table.
     * This method is kept for backward compatibility with existing code.
     *
     * @deprecated Use updateFullBackupStatus or updateIncrementalBackupStatus instead
     */
    @Deprecated
    public void updateBackupStatus(String batch_id, String status, Date businessDate, String ydbResponse) {
        if (updateDbackupStatus == null) {
            log.warn("Legacy updateDbackupStatus query not configured. Skipping legacy update.");
            return;
        }
        log.info("Updating the status of backup event for batch_id : {}", batch_id);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batch_id", batch_id);
            param.put("status", status);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("ydbResponse", ydbResponse);
            jdbcTemplate.update(updateDbackupStatus, param);
        } catch (Exception e) {
            log.error("Unable to update data in backup Table for batch_id : {}", batch_id, e);
            throw new DbBackupException("Error updating batch execution status for batch_id: " + batch_id, e);
        }
    }

    /**
     * Legacy method for storing base backup UUID in base_backup_uuid_tracker table.
     * This method is kept for backward compatibility with existing code.
     *
     * @deprecated Use updateFullBackupWithBaseUuid instead
     */
    @Deprecated
    public void storeBaseBackupUuid(String categoryCode, String baseBackupUuid, String backupMonth) {
        if (insertBaseBackupUuid == null) {
            log.warn("Legacy insertBaseBackupUuid query not configured. Skipping legacy store.");
            return;
        }
        log.info("Storing base backup UUID in legacy table for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("baseBackupUuid", baseBackupUuid);
            param.put("backupMonth", backupMonth);
            int rowsAffected = jdbcTemplate.update(insertBaseBackupUuid, param);
            log.info("Base backup UUID stored successfully in legacy table. Rows affected: {}", rowsAffected);
        } catch (Exception e) {
            log.error("Unable to store base backup UUID in legacy table for category: {}, month: {}", categoryCode, backupMonth, e);
            throw new DbBackupException("Error storing base backup UUID in legacy table for category: " + categoryCode, e);
        }
    }
}
