package com.scb.backup.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AppUtils - Utility class for common application operations.
 *
 * This class provides static utility methods for date formatting, batch parameter
 * creation, and other common operations used throughout the application.
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@Slf4j
public class AppUtils {

    /**
     * Converts a date string by removing hyphens.
     *
     * Transforms date format from "YYYY-MM-DD" to "YYYYMMDD".
     *
     * @param date Date string with hyphens (e.g., "2026-02-04")
     * @return Date string without hyphens (e.g., "20260204")
     */
    public static String getBusinessDate(String date) {
        return date.replace("-","");
    }

    /**
     * Parses a date string into a Date object.
     *
     * Uses the date pattern defined in {@link AppConstants#DATE_PATTERN}.
     * If parsing fails, logs the error and returns null.
     *
     * @param date Date string to parse (format: "yyyyMMdd")
     * @return Parsed Date object, or null if parsing fails
     */
    public static Date toDate(String date)  {
        SimpleDateFormat formatter = new SimpleDateFormat(AppConstants.DATE_PATTERN);
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            log.error("Exception in parsing date",e);
            return null;
        }
    }

    /**
     * Creates a batch parameters map for backup operations.
     *
     * Constructs a map containing batch ID, business date, and category code
     * required for backup processing.
     *
     * @param batchId Unique identifier for the batch execution
     * @param businessDate Business date in "yyyyMMdd" format
     * @param categoryCode Backup category code (e.g., "HWA_EPR_DB_BACKUP_FULL")
     * @return Map containing batch parameters with keys from {@link AppConstants}
     */
    public static Map<String, Object> createBatchParams(String batchId, String businessDate, String categoryCode) {
        Map<String, Object> batchParams = new HashMap<>();
        batchParams.put(AppConstants.BATCH_ID, batchId);
        batchParams.put(AppConstants.BUSINESS_DATE, AppUtils.toDate(businessDate));
        batchParams.put(AppConstants.CATEGORY_CODE, categoryCode);
        return batchParams;
    }
}
