package com.scb.backup.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * BackupCompletionEvent - Event published when a backup job completes (success or failure).
 *
 * This event is published by BackupPollerService when polling detects job completion.
 * BackupService listens to this event and updates the batch execution table accordingly.
 *
 * @author SCB ePricing Team
 * @version 1.0
 * @since 2026-02-10
 */
@Getter
public class BackupCompletionEvent extends ApplicationEvent {

    private final String batchId;
    private final String categoryCode;
    private final String businessDate;
    private final String backupMonth;
    private final boolean success;
    private final String errorMessage;

    /**
     * Constructor for successful backup completion.
     *
     * @param source The object that published the event
     * @param batchId Batch execution ID
     * @param categoryCode Backup category code
     * @param businessDate Business date in YYYYMMDD format
     * @param backupMonth Month in YYYY-MM format
     * @param success True if backup succeeded, false if failed
     * @param errorMessage Error message (null if success)
     */
    public BackupCompletionEvent(Object source, String batchId, String categoryCode, 
                                String businessDate, String backupMonth, boolean success, String errorMessage) {
        super(source);
        this.batchId = batchId;
        this.categoryCode = categoryCode;
        this.businessDate = businessDate;
        this.backupMonth = backupMonth;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Factory method for successful backup completion.
     */
    public static BackupCompletionEvent success(Object source, String batchId, String categoryCode,
                                               String businessDate, String backupMonth) {
        return new BackupCompletionEvent(source, batchId, categoryCode, businessDate, backupMonth, true, null);
    }

    /**
     * Factory method for failed backup completion.
     */
    public static BackupCompletionEvent failure(Object source, String batchId, String categoryCode,
                                               String businessDate, String backupMonth, String errorMessage) {
        return new BackupCompletionEvent(source, batchId, categoryCode, businessDate, backupMonth, false, errorMessage);
    }
}

