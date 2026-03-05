package com.scb.backup.exception;

import com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse;

import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @AfterEach
    void tearDown() {
    }


    @Test
    void handleGenericInServiceException_shouldReturnFailedStatus_whenExceptionOccurs2() {
        // Arrange: Use the real GlobalExceptionHandler
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        Throwable rootCause = new RuntimeException("Root cause");
        DbBackupException exception = new DbBackupException("File transfer failed", rootCause);

        // Act: Call the real method
        ResponseEntity<BatchStartResponse> response = exceptionHandler.handleFileTransferException(exception);

        // Assert: Validate response properties
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Assertions.assertEquals(AppConstants.BATCH_FAILED_STATUS, response.getBody().getExecutionStatus());
    }

}