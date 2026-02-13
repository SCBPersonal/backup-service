package com.scb.backup.controller;

import com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse;
import com.scb.backup.exception.DbBackupException;
import com.scb.backup.service.BackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupController Comprehensive Tests")
class BackupControllerTest {

    @Mock
    private BackupService backupService;

    @InjectMocks
    private BackupController backupController;

    private BatchStartResponse successResponse;
    private String validJsonRequest;

    @BeforeEach
    void setUp() {
        successResponse = BatchStartResponse.builder()
                .executionStatus("SUCCESS")
                .build();

        validJsonRequest = "{\"batchId\":\"BATCH_001\",\"categoryCode\":\"HWA_EPR_DB_BACKUP_FULL\",\"businessDate\":\"20260213\"}";
    }

    @Test
    @DisplayName("Should successfully process backup request")
    void should_ProcessBackup_When_ValidRequestProvided() throws Exception {
        // Given
        when(backupService.execute(anyString())).thenReturn(successResponse);

        // When
        Mono<BatchStartResponse> result = backupController.backupProcess(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(response -> "SUCCESS".equals(response.getExecutionStatus()))
                .verifyComplete();

        verify(backupService, times(1)).execute(validJsonRequest);
    }

    @Test
    @DisplayName("Should handle DbBackupException")
    void should_ThrowDbBackupException_When_ServiceFails() throws Exception {
        // Given
        when(backupService.execute(anyString()))
                .thenThrow(new DbBackupException("Database connection failed", new RuntimeException()));

        // When
        Mono<BatchStartResponse> result = backupController.backupProcess(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectError(DbBackupException.class)
                .verify();

        verify(backupService, times(1)).execute(validJsonRequest);
    }

    @Test
    @DisplayName("Should handle generic exception")
    void should_ThrowException_When_UnexpectedErrorOccurs() throws Exception {
        // Given
        when(backupService.execute(anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        Mono<BatchStartResponse> result = backupController.backupProcess(validJsonRequest);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(backupService, times(1)).execute(validJsonRequest);
    }

    @Test
    @DisplayName("Should handle null JSON request")
    void should_HandleNullRequest_When_JsonIsNull() throws Exception {
        // Given
        when(backupService.execute(null))
                .thenThrow(new IllegalArgumentException("Request cannot be null"));

        // When
        Mono<BatchStartResponse> result = backupController.backupProcess(null);

        // Then
        StepVerifier.create(result)
                .expectError(DbBackupException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle empty JSON request")
    void should_HandleEmptyRequest_When_JsonIsEmpty() throws Exception {
        // Given
        String emptyJson = "{}";
        when(backupService.execute(emptyJson))
                .thenThrow(new IllegalArgumentException("Invalid request"));

        // When
        Mono<BatchStartResponse> result = backupController.backupProcess(emptyJson);

        // Then
        StepVerifier.create(result)
                .expectError(DbBackupException.class)
                .verify();
    }
}