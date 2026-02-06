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

    @Value("${data.update-full-backup-with-base-uuid.query}")
    String updateFullBackupWithBaseUuid;

    @Value("${data.update-full-backup-status.query}")
    String updateFullBackupStatus;

    @Value("${data.get-full-backup-base-uuid.query}")
    String getFullBackupBaseUuid;

    @Value("${data.get-full-backup-by-task-uuid.query}")
    String getFullBackupByTaskUuid;

    @Value("${data.get-pending-full-backups.query}")
    String getPendingFullBackups;

    // Incremental Backup Table Queries
    @Value("${data.insert-incremental-backup.query}")
    String insertIncrementalBackup;

    @Value("${data.update-incremental-backup-status.query}")
    String updateIncrementalBackupStatus;

    // ==================== FULL BACKUP TABLE OPERATIONS ====================

    /**
     * Inserts a new full backup record into the database.
     *
     * This method is called immediately after triggering a full backup via YBA API.
     * It stores the initial backup details including the YBA response and task UUID
     * for later polling.
     *
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param businessDate Business date
     * @param backupMonth Month in YYYY-MM format
     * @param fullBackupResponse YBA API response JSON
     * @param taskUuid YBA task UUID for polling
     * @throws DbBackupException if insert fails
     */
    public void insertFullBackupRecord(String batchId, String categoryCode, Date businessDate,
                                       String backupMonth, String fullBackupResponse, String taskUuid) {
        log.info("Inserting full backup record for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batchId", batchId);
            param.put("categoryCode", categoryCode);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("backupMonth", backupMonth);
            param.put("backupStatus", AppConstants.BACKUP_INPROGRESS_STATUS);
            param.put("fullBackupResponse", fullBackupResponse);
            param.put("taskUuid", taskUuid);
            param.put("startTime", Timestamp.valueOf(LocalDateTime.now()));

            jdbcTemplate.update(insertFullBackup, param);
            log.info("Full backup record inserted successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Error inserting full backup record for category: {}", categoryCode, e);
            throw new DbBackupException("Error inserting full backup record for category: " + categoryCode, e);
        }
    }

    /**
     * Updates the full backup record with base UUID after job completion.
     *
     * This method is called by the poller when the full backup job completes successfully.
     * It updates the record with the base backup UUID fetched from YBA's last backup API.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param baseBackupUuid Base backup UUID from YBA
     * @throws DbBackupException if update fails
     */
    public void updateFullBackupWithBaseUuid(String categoryCode, String backupMonth, String baseBackupUuid) {
        log.info("Updating full backup with base UUID for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);
            param.put("baseBackupUuid", baseBackupUuid);
            param.put("backupStatus", AppConstants.BACKUP_SUCCESS_STATUS);
            param.put("endTime", Timestamp.valueOf(LocalDateTime.now()));

            jdbcTemplate.update(updateFullBackupWithBaseUuid, param);
            log.info("Full backup updated with base UUID: {} for category: {}", baseBackupUuid, categoryCode);
        } catch (Exception e) {
            log.error("Error updating full backup with base UUID for category: {}", categoryCode, e);
            throw new DbBackupException("Error updating full backup with base UUID for category: " + categoryCode, e);
        }
    }

    /**
     * Updates the full backup status (for failures or other status changes).
     *
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param status New backup status
     * @param errorMessage Error message (if any)
     * @throws DbBackupException if update fails
     */
    public void updateFullBackupStatus(String categoryCode, String backupMonth, String status, String errorMessage) {
        log.info("Updating full backup status to {} for category: {}, month: {}", status, categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);
            param.put("backupStatus", status);
            param.put("errorMessage", errorMessage);
            param.put("endTime", Timestamp.valueOf(LocalDateTime.now()));

            jdbcTemplate.update(updateFullBackupStatus, param);
            log.info("Full backup status updated to {} for category: {}", status, categoryCode);
        } catch (Exception e) {
            log.error("Error updating full backup status for category: {}", categoryCode, e);
            throw new DbBackupException("Error updating full backup status for category: " + categoryCode, e);
        }
    }

    /**
     * Retrieves the base backup UUID from the full backup table.
     *
     * This method is used by incremental backup operations to get the base UUID
     * from the full backup table instead of the old base_backup_uuid_tracker table.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @return Base backup UUID if found, null otherwise
     * @throws DbBackupException if query fails
     */
    public String getBaseBackupUuidFromFullBackupTable(String categoryCode, String backupMonth) {
        log.info("Retrieving base backup UUID from full backup table for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);

            String baseUuid = jdbcTemplate.queryForObject(getFullBackupBaseUuid, param, String.class);
            log.info("Base backup UUID retrieved from full backup table: {}", baseUuid);
            return baseUuid;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("No base backup UUID found in full backup table for category: {}, month: {}", categoryCode, backupMonth);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving base backup UUID from full backup table for category: {}", categoryCode, e);
            throw new DbBackupException("Error retrieving base backup UUID from full backup table for category: " + categoryCode, e);
        }
    }

    // ==================== INCREMENTAL BACKUP TABLE OPERATIONS ====================

    /**
     * Inserts a new incremental backup record into the database.
     *
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param businessDate Business date
     * @param backupMonth Month in YYYY-MM format
     * @param baseBackupUuid Base backup UUID from full backup
     * @param incrementalBackupResponse YBA API response JSON
     * @param taskUuid YBA task UUID
     * @throws DbBackupException if insert fails
     */
    public void insertIncrementalBackupRecord(String batchId, String categoryCode, Date businessDate,
                                              String backupMonth, String baseBackupUuid,
                                              String incrementalBackupResponse, String taskUuid) {
        log.info("Inserting incremental backup record for category: {}, base UUID: {}", categoryCode, baseBackupUuid);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batchId", batchId);
            param.put("categoryCode", categoryCode);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("backupMonth", backupMonth);
            param.put("baseBackupUuid", baseBackupUuid);
            param.put("backupStatus", AppConstants.BACKUP_INPROGRESS_STATUS);
            param.put("incrementalBackupResponse", incrementalBackupResponse);
            param.put("taskUuid", taskUuid);
            param.put("startTime", Timestamp.valueOf(LocalDateTime.now()));

            jdbcTemplate.update(insertIncrementalBackup, param);
            log.info("Incremental backup record inserted successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Error inserting incremental backup record for category: {}", categoryCode, e);
            throw new DbBackupException("Error inserting incremental backup record for category: " + categoryCode, e);
        }
    }

    /**
     * Updates the incremental backup status.
     *
     * @param batchId Batch execution ID
     * @param businessDate Business date
     * @param status New backup status
     * @param errorMessage Error message (if any)
     * @throws DbBackupException if update fails
     */
    public void updateIncrementalBackupStatus(String batchId, Date businessDate, String status, String errorMessage) {
        log.info("Updating incremental backup status to {} for batch: {}", status, batchId);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batchId", batchId);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("backupStatus", status);
            param.put("errorMessage", errorMessage);
            param.put("endTime", Timestamp.valueOf(LocalDateTime.now()));

            jdbcTemplate.update(updateIncrementalBackupStatus, param);
            log.info("Incremental backup status updated to {} for batch: {}", status, batchId);
        } catch (Exception e) {
            log.error("Error updating incremental backup status for batch: {}", batchId, e);
            throw new DbBackupException("Error updating incremental backup status for batch: " + batchId, e);
        }
    }
}
