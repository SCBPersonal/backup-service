package com.scb.backup.utils;

/**
 * AppConstants - Application-wide constant definitions.
 *
 * This class contains all constant values used throughout the backup orchestrator
 * application including status codes, parameter names, date formats, and other
 * configuration values.
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
public class AppConstants {

    // Batch Execution Status Constants
    /** Batch execution completed successfully */
    public static final String BATCH_COMPLETED_STATUS="COMPLETED";

    /** Batch execution failed */
    public static final String BATCH_FAILED_STATUS="FAILED";

    // Backup Response Constants
    /** Key for backup job category in response */
    public static final String BACKUP_JOB_CATEGORY="public static final String";

    /** Key for backup response data */
    public static final String BACKUP_RESPONSE="backup_response";

    /** Key for error message in response */
    public static final String ERROR_MESSAGE = "error_message";

    // Backup Type Constants
    /** Identifier for full backup operations */
    public static final String FULL_BACKUP= "full_backup";

    /** Key for payload data */
    public static final String PAYLOAD= "payload";

    /** Identifier for incremental backup operations */
    public static final String INCREMENTAL_BACKUP="incremental_backup";

    /** Key for backup type parameter */
    public static final String BACKUP_TYPE="backup_type";

    // Error Constants
    /** Generic technical error message */
    public static final String ERROR_DETAIL= "Technical Error";

    // Backup Status Constants
    /** Backup operation start time key */
    public static final String START_TIME = "start_time";

    /** Backup operation in progress status */
    public static final String BACKUP_INPROGRESS_STATUS = "IN_PROGRESS";

    /** Backup operation success status */
    public static final String BACKUP_SUCCESS_STATUS="SUCCESS";

    /** Backup operation failed status */
    public static final String BACKUP_FAILED_STATUS="FAILED";

    /** Backup operation end time key */
    public static final String END_TIME = "end_time";

    // Date and Time Constants
    /** Default timezone for backup operations */
    public static final String TIMEZONE ="Asia/Kolkata";

    /** Date format pattern for business dates */
    public static final String DATE_PATTERN = "yyyyMMdd";

    // Batch Parameter Keys
    /** Key for batch ID parameter */
    public static final String BATCH_ID = "batch_id";

    /** Key for business date parameter */
    public static final String BUSINESS_DATE = "businessDate";

    /** Key for batch category code parameter */
    public static final String CATEGORY_CODE = "batchCategoryCode";

    /** Key for sub-category code parameter */
    public static final String SUB_CATEGORY_CODE = "subBatchCategoryCode";

}
