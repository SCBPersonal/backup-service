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

@Slf4j
@Repository
@RequiredArgsConstructor
public class BackupDaoService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${data.db-schedule-backup-insert.query}")
    String insertScheduleBackup;

    @Value("${data.update-schedule-backup.query}")
    String updateDbackupStatus;
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
}
