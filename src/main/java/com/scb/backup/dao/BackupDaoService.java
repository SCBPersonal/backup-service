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

    @Value("${data.db-schedule-backup-insert.query}")
    String insertScheduleBackup;

    @Value("${data.update-schedule-backup.query}")
    String updateDbackupStatus;

    @Value("${data.insert-base-backup-uuid.query}")
    String insertBaseBackupUuid;

    @Value("${data.get-base-backup-uuid.query}")
    String getBaseBackupUuid;

    public void insertBackupDetails(Map<String,Object> backupDetails,String backupStatus,String backupType) {
        log.info("Inserting data in Backup Table:");
        try {
            backupDetails.put("batch_id",backupDetails.get(AppConstants.BATCH_ID));
            backupDetails.put("batchCategory",backupDetails.get(AppConstants.CATEGORY_CODE));
            backupDetails.put("backupStatus",AppConstants.BACKUP_INPROGRESS_STATUS);
            backupDetails.put("backupType",backupType);
            backupDetails.put("business_date",AppConstants.BUSINESS_DATE);
            backupDetails.put("start_time", Timestamp.valueOf(LocalDateTime.now()));
            jdbcTemplate.update(insertScheduleBackup,backupDetails);
        } catch (Exception e) {
            log.error("Unable to Insert data in Backup Table ",e);
            throw new DbBackupException("Unable to Insert data in Backup Table",e);
        }
    }

    public void updateBackupStatus(String batch_id, String status, Date businessDate,String ydbResponse) {
        log.info("Updating the status of backup event for batch_id : {}",batch_id);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("batch_id", batch_id);
            param.put("status", status);
            param.put("businessDate", new java.sql.Date(businessDate.getTime()));
            param.put("ydbResponse",ydbResponse);
            jdbcTemplate.update(updateDbackupStatus, param);    }
        catch (Exception e) {
            log.error("Unable to update data in backup Table for batch_id : {}",batch_id,e);
            throw new DbBackupException("Error updating batch execution status for batch_id: {}" + batch_id, e);
        }
    }

    /**
     * Stores the base backup UUID for a given category and month.
     *
     * This method uses an UPSERT operation (INSERT ... ON CONFLICT DO UPDATE) to ensure
     * that only one base backup UUID exists per category per month. If a record already
     * exists for the given category and month, it will be updated with the new UUID.
     *
     * This is critical for incremental backups, as they require a base backup UUID
     * from the same month to function properly.
     *
     * @param categoryCode The backup category code (e.g., "HWA_EPR_DB_BACKUP_FULL")
     * @param baseBackupUuid The base backup UUID returned from YBA API
     * @param backupMonth The month in YYYY-MM format (e.g., "2026-02")
     * @throws DbBackupException if database operation fails
     */
    public void storeBaseBackupUuid(String categoryCode, String baseBackupUuid, String backupMonth) {
        log.info("Storing base backup UUID for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("baseBackupUuid", baseBackupUuid);
            param.put("backupMonth", backupMonth);
            int rowsAffected = jdbcTemplate.update(insertBaseBackupUuid, param);
            log.info("Base backup UUID stored successfully. Rows affected: {}", rowsAffected);
        } catch (Exception e) {
            log.error("Unable to store base backup UUID for category: {}, month: {}", categoryCode, backupMonth, e);
            throw new DbBackupException("Error storing base backup UUID for category: " + categoryCode, e);
        }
    }

    /**
     * Retrieves the base backup UUID for a given category and month from the database.
     *
     * This method queries the database for the most recent base backup UUID for the
     * specified category and month. It is used by incremental backup operations to
     * obtain the base backup UUID required for the YBA API call.
     *
     * If no record is found (EmptyResultDataAccessException), the method returns null
     * instead of throwing an exception, allowing the caller to handle the missing UUID
     * appropriately (typically by throwing an IllegalStateException).
     *
     * @param categoryCode The backup category code (e.g., "HWA_EPR_DB_BACKUP_INCRE")
     * @param backupMonth The month in YYYY-MM format (e.g., "2026-02")
     * @return The base backup UUID if found, null otherwise
     * @throws DbBackupException if database query fails (excluding EmptyResultDataAccessException)
     */
    public String getBaseBackupUuidFromDb(String categoryCode, String backupMonth) {
        log.info("Retrieving base backup UUID for category: {}, month: {}", categoryCode, backupMonth);
        Map<String, Object> param = new HashMap<>();
        try {
            param.put("categoryCode", categoryCode);
            param.put("backupMonth", backupMonth);

            String baseUuid = jdbcTemplate.queryForObject(getBaseBackupUuid, param, String.class);
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
}
