package com.scb.backup.exception;

import com.scb.backup.utils.AppConstants;
import com.scb.epricing.batch.core.lib.model.BatchStartResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler class responsible for handling all exceptions
 * thrown by controllers in the application. It provides custom responses
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DbBackupException.class)
    public ResponseEntity<BatchStartResponse> handleFileTransferException(DbBackupException e) {
        log.error("File transfer failed due to technical error: {}", e.getMessage(), e);
        Map<String,Object> extensionFields = new HashMap<>();
        extensionFields.put(AppConstants.ERROR_MESSAGE,AppConstants.ERROR_DETAIL);
        BatchStartResponse errorResponse = new BatchStartResponse();
        errorResponse.setExecutionStatus(AppConstants.BATCH_FAILED_STATUS);
        errorResponse.setExtensionFields(extensionFields);
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }

}
