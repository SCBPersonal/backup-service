package com.scb.backup.exception;

import com.scb.backup.utils.AppConstants;
import com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - Centralized exception handling for the application.
 *
 * This class intercepts exceptions thrown by REST controllers and converts them
 * into appropriate HTTP responses. It ensures consistent error handling across
 * the application and provides meaningful error messages to clients.
 *
 * <p><b>Handled Exceptions:</b></p>
 * <ul>
 *   <li>{@link DbBackupException} - Technical errors during backup operations</li>
 * </ul>
 *
 * <p>All exceptions are logged with full stack traces for troubleshooting while
 * returning user-friendly error responses to the client.</p>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see DbBackupException
 * @see BatchStartResponse
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles DbBackupException thrown during backup operations.
     *
     * This method catches technical errors that occur during backup processing
     * and converts them into a standardized batch response with FAILED status.
     * The error details are logged for troubleshooting.
     *
     * @param e The DbBackupException containing error details
     * @return ResponseEntity containing BatchStartResponse with FAILED status and error message
     */
    @ExceptionHandler(DbBackupException.class)
    public ResponseEntity<BatchStartResponse> handleFileTransferException(DbBackupException e) {
        log.error("File transfer failed due to technical error: {}", e.getMessage(), e);
        Map<String,Object> extensionFields = new HashMap<>();
        extensionFields.put(AppConstants.ERROR_MESSAGE,e.getMessage());
        BatchStartResponse errorResponse = new BatchStartResponse();
        errorResponse.setExecutionStatus(AppConstants.BATCH_FAILED_STATUS);
        errorResponse.setExtensionFields(extensionFields);
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }

}
