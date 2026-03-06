package com.scb.backup.dao;

import com.scb.backup.exception.DbBackupException;
import com.scb.backup.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * BackupDaoService - Data Access Object for backup operations.
 *
 * This service handles all database operations related to backup tracking,
 * including backup status management and base backup UUID storage for
 * incremental backups based on configurable backup frequency (monthly, weekly, or custom intervals).
 *
 * The backup_period column stores the backup period in different formats:
 * - MONTHLY: YYYY-MM (e.g., "2026-03")
 * - WEEKLY: YYYY-Www (e.g., "2026-W10")
 * - CUSTOM: YYYY-MM-DD (e.g., "2026-03-11")
 *
 * @author SCB ePricing Team
 * @version 3.0
 * @since 2026-03-05
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BackupDaoService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${data.insert-full-backup.query}")
    String insertFullBackup;

    @Value("${data.update-full-backup-with-base-uuid.query}")
    String updateFullBackupWithBaseUuidQuery;

    @Value("${data.get-base-backup-uuid-from-full-tracker.query}")
    String getBaseBackupUuidFromFullTracker;

    // Incremental Backup Table Queries
    @Value("${data.insert-incremental-backup.query}")
    String insertIncrementalBackup;

    @Value("${data.update-incremental-backup-status.query}")
    String updateIncrementalBackupStatusQuery;



    /**
     * Inserts a new full backup record into the full_backup_tracker table with response.
     *
     * @param param Batch parameters including batch_id, category_code, business_date
     * @param backupMonth Backup period in format based on frequency type:
     *                    MONTHLY: YYYY-MM (e.g., "2026-03")
     *                    WEEKLY: YYYY-Www (e.g., "2026-W10")
     *                    CUSTOM: YYYY-MM-DD (e.g., "2026-03-05")
     * @param backupInterval Backup frequency interval (e.g., "MONTHLY", "WEEKLY", "10_DAYS")
     * @param taskUuid YBA task UUID for polling
     * @param fullBackupResponse YBA API response JSON
     * @param dbName Database name
     * @throws DbBackupException if database operation fails
     */
    public void insertFullBackupRecord(Map<String,Object> param,
                                       String backupMonth, String backupInterval, String taskUuid, String fullBackupResponse,String dbName) {
        log.info("Inserting full backup record with response for dbName: {}, month: {}, interval: {}", dbName, backupMonth, backupInterval);
        String categoryCode = (String) param.get(AppConstants.CATEGORY_CODE);
        try {

            param.put(AppConstants.BACKUP_PERIOD, backupMonth);
            param.put(AppConstants.BACKUP_INTERVAL, backupInterval);
            param.put(AppConstants.BACKUP_STATUS, AppConstants.BACKUP_INPROGRESS_STATUS);
            param.put(AppConstants.TASKUUID, taskUuid);
            param.put(AppConstants.FULL_BACKUP_RESPONSE, fullBackupResponse);
            param.put(AppConstants.DATABASE_NAME,dbName);

            jdbcTemplate.update(insertFullBackup, param);
            log.info("Full backup record with response inserted successfully for category: {} with interval: {}", categoryCode, backupInterval);
        } catch (Exception e) {
            log.error("Unable to insert full backup record for category: {}", categoryCode, e);
            throw new DbBackupException("Error inserting full backup record for category: " + categoryCode, e);
        }
    }


    /**
     * Updates the full backup record with base backup UUID and marks it as SUCCESS.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Backup period in format based on frequency type:
     *                    MONTHLY: YYYY-MM (e.g., "2026-03")
     *                    WEEKLY: YYYY-Www (e.g., "2026-W10")
     *                    CUSTOM: YYYY-MM-DD (e.g., "2026-03-05")
     * @param baseBackupUuid Base backup UUID from YBA
     * @param batchId Batch execution ID
     * @param backupStatus Backup status (SUCCESS, FAILED)
     * @throws DbBackupException if database operation fails
     */
    public void updateFullBackupWithBaseUuid(String categoryCode, String backupMonth, String baseBackupUuid,String batchId,String backupStatus) {
        log.info("Updating full backup with base UUID for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put(AppConstants.CATEGORY, categoryCode);
            param.put(AppConstants.BACKUP_PERIOD, backupMonth);
            param.put(AppConstants.BASE_BACKUP_UUID, baseBackupUuid);
            param.put(AppConstants.BACKUP_BATCH_ID,batchId);
            param.put(AppConstants.BACKUP_STATUS,backupStatus);

            jdbcTemplate.update(updateFullBackupWithBaseUuidQuery, param);
            log.info("Full backup updated with base UUID successfully for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Unable to update full backup with base UUID for category: {}", categoryCode, e);
            throw new DbBackupException("Error updating full backup with base UUID for category: " + categoryCode, e);
        }
    }

    /**
     * Retrieves the base backup UUID from full_backup_tracker for a given backup period and database.
     *
     * This method queries for the most recent successful full backup within the specified period.
     *
     * @param backupPeriod Backup period in format based on frequency type:
     *                     MONTHLY: YYYY-MM (e.g., "2026-03")
     *                     WEEKLY: YYYY-Www (e.g., "2026-W11")
     *                     CUSTOM: YYYY-MM-DD (e.g., "2026-03-11")
     * @param dbName Database name
     * @return Base backup UUID if found, empty string otherwise
     * @throws DbBackupException if database query fails
     */
    public String getBaseBackupUuidFromDb(String backupPeriod, String dbName) {
        log.info("Retrieving base backup UUID for period: {}, database: {}", backupPeriod, dbName);
        Map<String, Object> param = new HashMap<>();
        try {

            param.put(AppConstants.BACKUP_PERIOD, backupPeriod);
            param.put(AppConstants.DATABASE_NAME, dbName);

            String baseUuid = jdbcTemplate.queryForObject(getBaseBackupUuidFromFullTracker, param, String.class);
            log.info("Base backup UUID retrieved: {}", baseUuid);
            return baseUuid;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("No base backup UUID found for database: {}, period: {}", dbName, backupPeriod);
            return "";
        } catch (Exception e) {
            log.error("Error retrieving base backup UUID for database: {}, period: {}", dbName, backupPeriod, e);
            throw new DbBackupException("Error retrieving base backup UUID for database: " + dbName, e);
        }
    }

    // ==================== INCREMENTAL BACKUP TABLE OPERATIONS ====================

    /**
     * Inserts a new incremental backup record into the incremental_backup_tracker table with response.
     *
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param businessDate Business date
     * @param backupMonth Backup period in format based on frequency type:
     *                    MONTHLY: YYYY-MM (e.g., "2026-03")
     *                    WEEKLY: YYYY-Www (e.g., "2026-W10")
     *                    CUSTOM: YYYY-MM-DD (e.g., "2026-03-05")
     * @param backupInterval Backup frequency interval (e.g., "MONTHLY", "WEEKLY", "10_DAYS")
     * @throws DbBackupException if database operation fails
     */
    public void insertIncrementalBackupRecord(String batchId, String categoryCode, Date businessDate,
                                              String backupMonth, String backupInterval) {
        log.info("Inserting incremental backup record with response for category: {}, period: {}, interval: {}", categoryCode, backupMonth, backupInterval);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put(AppConstants.BACKUP_BATCH_ID, batchId);
            param.put(AppConstants.CATEGORY, categoryCode);
            param.put(AppConstants.BUSINESS_DATE, new java.sql.Date(businessDate.getTime()));
            param.put(AppConstants.BACKUP_PERIOD, backupMonth);
            param.put(AppConstants.BACKUP_INTERVAL, backupInterval);
            param.put(AppConstants.BACKUP_STATUS, AppConstants.BACKUP_INPROGRESS_STATUS);


            jdbcTemplate.update(insertIncrementalBackup, param);
            log.info("Incremental backup record with response inserted successfully for category: {} with interval: {}", categoryCode, backupInterval);
        } catch (Exception e) {
            log.error("Unable to insert incremental backup record for category: {}", categoryCode, e);
            throw new DbBackupException("Error inserting incremental backup record for category: " + categoryCode, e);
        }
    }


    /**
     * Updates the status of an incremental backup by category code, backup period, and base UUID.
     *
     * @param categoryCode Backup category code
     * @param backupMonth Backup period in format based on frequency type:
     *                    MONTHLY: YYYY-MM (e.g., "2026-03")
     *                    WEEKLY: YYYY-Www (e.g., "2026-W10")
     *                    CUSTOM: YYYY-MM-DD (e.g., "2026-03-05")
     * @param baseUuid Base backup UUID
     * @param status Backup status (SUCCESS, FAILED)
     * @param batchId Batch execution ID
     * @param response Backup response JSON
     * @param taskUuid YBA task UUID
     * @throws DbBackupException if database operation fails
     */
    public void updateIncrementalBackupStatusByMonth(String categoryCode, String backupMonth, String baseUuid,
                                                     String status, String batchId, String response, String taskUuid) {
        log.info("Updating incremental backup status for category: {}, month: {}, baseUuid: {}, status: {}",
                categoryCode, backupMonth, baseUuid, status);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put(AppConstants.CATEGORY, categoryCode);
            param.put(AppConstants.BACKUP_PERIOD, backupMonth);
            param.put(AppConstants.BASE_BACKUP_UUID, baseUuid);
            param.put(AppConstants.BACKUP_STATUS, status);
            param.put(AppConstants.BACKUP_BATCH_ID,batchId);
            param.put(AppConstants.TASKUUID,taskUuid);
            param.put(AppConstants.BACKUP_RESPONSE,response);

            jdbcTemplate.update(updateIncrementalBackupStatusQuery, param);
            log.info("Incremental backup status updated successfully for category: {}, month: {}", categoryCode, backupMonth);
        } catch (Exception e) {
            log.error("Unable to update incremental backup status for category: {}, month: {}", categoryCode, backupMonth, e);
            throw new DbBackupException("Error updating incremental backup status for category: " + categoryCode, e);
        }
    }
}
