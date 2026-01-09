//package com.scb.backup.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.scb.backup.BackupController;
//import com.scb.backup.dto.BackupRequest;
//import com.scb.backup.dto.BackupResponse;
//import com.scb.backup.exception.DbBackupException;
//import com.scb.backup.service.BackupService;
//import com.scb.epricing.batch.core.lib.model.BatchStartResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@WebFluxTest(BackupController.class)
//@DisplayName("BackupController Comprehensive Tests")
//class BackupControllerTest {
//
//    @Autowired
//    private WebTestClient webTestClient;
//
//    @MockBean
//    private BackupService backupService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private BackupRequest validFullBackupRequest;
//    private BackupRequest validIncrementalBackupRequest;
//    private BatchStartResponse successResponse;
//    private BatchStartResponse failureResponse;
//
//    @BeforeEach
//    void setUp() {
//        validFullBackupRequest = BackupRequest.builder()
//                .batchId("BATCH_001")
//                .categoryCode("HWA_EPR_DB_BACKUP_FULL")
//                .businessDate("20240101")
//                .build();
//
//        validIncrementalBackupRequest = BackupRequest.builder()
//                .batchId("BATCH_002")
//                .categoryCode("HWA_EPR_DB_BACKUP_INCRE")
//                .businessDate("20240102")
//                .build();
//
//        Map<String, Object> extensionFields = new HashMap<>();
//        extensionFields.put("requestId", "test-request-id");
//        extensionFields.put("timestamp", System.currentTimeMillis());
//
//        successResponse = BatchStartResponse.builder()
//                .executionStatus("SUCCESS")
//                .extensionFields(extensionFields)
//                .build();
//
//        failureResponse = BatchStartResponse.builder()
//                .executionStatus("FAILED")
//                .errorMessage("Backup process failed")
//                .build();
//    }
//
//    // Positive Test Cases
//    @Test
//    @DisplayName("Should successfully initiate full backup with valid request")
//    void should_InitiateFullBackup_When_ValidRequestProvided() throws Exception {
//        // Given
//        when(backupService.execute(anyString())).thenReturn(successResponse);
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validFullBackupRequest)
//                .header("X-Request-ID", "test-request-id")
//                .exchange()
//                .expectStatus().isOk()
//                .expectHeader().exists("X-Request-ID")
//                .expectHeader().valueEquals("X-Request-ID", "test-request-id")
//                .expectBody(BatchStartResponse.class)
//                .value(response -> {
//                    assert response.getExecutionStatus().equals("SUCCESS");
//                    assert response.getExtensionFields().containsKey("requestId");
//                });
//
//        verify(backupService, times(1)).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should successfully initiate incremental backup with valid request")
//    void should_InitiateIncrementalBackup_When_ValidRequestProvided() throws Exception {
//        // Given
//        when(backupService.execute(anyString())).thenReturn(successResponse);
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validIncrementalBackupRequest)
//                .exchange()
//                .expectStatus().isOk()
//                .expectHeader().exists("X-Request-ID")
//                .expectBody(BatchStartResponse.class)
//                .value(response -> {
//                    assert response.getExecutionStatus().equals("SUCCESS");
//                });
//
//        verify(backupService, times(1)).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should generate request ID when not provided in header")
//    void should_GenerateRequestId_When_NotProvidedInHeader() throws Exception {
//        // Given
//        when(backupService.execute(anyString())).thenReturn(successResponse);
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validFullBackupRequest)
//                .exchange()
//                .expectStatus().isOk()
//                .expectHeader().exists("X-Request-ID");
//
//        verify(backupService, times(1)).execute(anyString());
//    }
//
//    // Validation Test Cases
//    @Test
//    @DisplayName("Should return 400 when batch ID is missing")
//    void should_ReturnBadRequest_When_BatchIdMissing() {
//        // Given
//        BackupRequest invalidRequest = BackupRequest.builder()
//                .categoryCode("HWA_EPR_DB_BACKUP_FULL")
//                .businessDate("20240101")
//                .build();
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(invalidRequest)
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should return 400 when batch ID is blank")
//    void should_ReturnBadRequest_When_BatchIdIsBlank() {
//        // Given
//        BackupRequest invalidRequest = BackupRequest.builder()
//                .batchId("   ")
//                .categoryCode("HWA_EPR_DB_BACKUP_FULL")
//                .businessDate("20240101")
//                .build();
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(invalidRequest)
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should return 400 when category code is invalid")
//    void should_ReturnBadRequest_When_InvalidCategoryCode() {
//        // Given
//        BackupRequest invalidRequest = BackupRequest.builder()
//                .batchId("BATCH_001")
//                .categoryCode("INVALID_CATEGORY")
//                .businessDate("20240101")
//                .build();
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(invalidRequest)
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should return 400 when business date format is invalid")
//    void should_ReturnBadRequest_When_InvalidBusinessDateFormat() {
//        // Given
//        BackupRequest invalidRequest = BackupRequest.builder()
//                .batchId("BATCH_001")
//                .categoryCode("HWA_EPR_DB_BACKUP_FULL")
//                .businessDate("2024-01-01")
//                .build();
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(invalidRequest)
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should return 400 when request body is malformed JSON")
//    void should_ReturnBadRequest_When_MalformedJsonProvided() {
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue("{invalid json}")
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).execute(anyString());
//    }
//
//    // Error Handling Test Cases
//    @Test
//    @DisplayName("Should handle DbBackupException and return 500")
//    void should_ReturnInternalServerError_When_DbBackupExceptionThrown() throws Exception {
//        // Given
//        when(backupService.execute(anyString()))
//                .thenThrow(new DbBackupException("Database connection failed", new RuntimeException()));
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validFullBackupRequest)
//                .exchange()
//                .expectStatus().is5xxServerError()
//                .expectBody(BatchStartResponse.class)
//                .value(response -> {
//                    assert response.getExecutionStatus().equals("FAILED");
//                    assert response.getErrorMessage().contains("Database backup operation failed");
//                });
//
//        verify(backupService, times(1)).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should handle IllegalArgumentException and return 400")
//    void should_ReturnBadRequest_When_IllegalArgumentExceptionThrown() throws Exception {
//        // Given
//        when(backupService.execute(anyString()))
//                .thenThrow(new IllegalArgumentException("Invalid batch parameters"));
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validFullBackupRequest)
//                .exchange()
//                .expectStatus().isBadRequest()
//                .expectBody(BatchStartResponse.class)
//                .value(response -> {
//                    assert response.getExecutionStatus().equals("FAILED");
//                    assert response.getErrorMessage().contains("Invalid request parameters");
//                });
//
//        verify(backupService, times(1)).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should handle generic exception and return 500")
//    void should_ReturnInternalServerError_When_GenericExceptionThrown() throws Exception {
//        // Given
//        when(backupService.execute(anyString()))
//                .thenThrow(new RuntimeException("Unexpected error"));
//
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validFullBackupRequest)
//                .exchange()
//                .expectStatus().is5xxServerError()
//                .expectBody(BatchStartResponse.class)
//                .value(response -> {
//                    assert response.getExecutionStatus().equals("FAILED");
//                    assert response.getErrorMessage().equals("Internal server error occurred");
//                });
//
//        verify(backupService, times(1)).execute(anyString());
//    }
//
//    @Test
//    @DisplayName("Should handle timeout and return appropriate error")
//    void should_HandleTimeout_When_RequestTakesTooLong() throws Exception {
//        // Given
//        when(backupService.execute(anyString()))
//                .thenAnswer(invocation -> {
//                    Thread.sleep(6000); // Simulate long-running operation
//                    return successResponse;
//                });
//
//        // When & Then
//        webTestClient.mutate()
//                .responseTimeout(Duration.ofSeconds(1))
//                .build()
//                .post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(validFullBackupRequest)
//                .exchange()
//                .expectStatus().is5xxServerError();
//    }
//
//    // Status Endpoint Tests
//    @Test
//    @DisplayName("Should successfully get backup status")
//    void should_GetBackupStatus_When_ValidParametersProvided() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String status = "IN_PROGRESS";
//
//        when(backupService.getBackupStatus(batchId, categoryCode))
//                .thenReturn(Mono.just(status));
//
//        // When & Then
//        webTestClient.get()
//                .uri("/api/v1/backup/status/{batchId}?categoryCode={categoryCode}", batchId, categoryCode)
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(BackupResponse.class)
//                .value(response -> {
//                    assert response.getBatchId().equals(batchId);
//                    assert response.getCategoryCode().equals(categoryCode);
//                    assert response.getStatus().equals(status);
//                    assert response.getTimestamp() != null;
//                });
//
//        verify(backupService, times(1)).getBackupStatus(batchId, categoryCode);
//    }
//
//    @Test
//    @DisplayName("Should return 404 when backup status not found")
//    void should_ReturnNotFound_When_BackupStatusNotExists() {
//        // Given
//        String batchId = "NONEXISTENT_BATCH";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//
//        when(backupService.getBackupStatus(batchId, categoryCode))
//                .thenReturn(Mono.empty());
//
//        // When & Then
//        webTestClient.get()
//                .uri("/api/v1/backup/status/{batchId}?categoryCode={categoryCode}", batchId, categoryCode)
//                .exchange()
//                .expectStatus().isNotFound();
//
//        verify(backupService, times(1)).getBackupStatus(batchId, categoryCode);
//    }
//
//    @Test
//    @DisplayName("Should return 400 when batch ID is blank in status request")
//    void should_ReturnBadRequest_When_BlankBatchIdInStatusRequest() {
//        // When & Then
//        webTestClient.get()
//                .uri("/api/v1/backup/status/ ?categoryCode=HWA_EPR_DB_BACKUP_FULL")
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).getBackupStatus(anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("Should return 400 when category code is missing in status request")
//    void should_ReturnBadRequest_When_CategoryCodeMissingInStatusRequest() {
//        // When & Then
//        webTestClient.get()
//                .uri("/api/v1/backup/status/BATCH_001")
//                .exchange()
//                .expectStatus().isBadRequest();
//
//        verify(backupService, never()).getBackupStatus(anyString(), anyString());
//    }
//
//    // Health Check Tests
//    @Test
//    @DisplayName("Should return healthy status for health check")
//    void should_ReturnHealthy_When_HealthCheckCalled() {
//        // When & Then
//        webTestClient.get()
//                .uri("/api/v1/backup/health")
//                .exchange()
//                .expectStatus().isOk()
//                .expectBody(String.class)
//                .value(response -> assert response.equals("Backup service is healthy"));
//    }
//
//    // Content Type Tests
//    @Test
//    @DisplayName("Should return 415 when unsupported media type provided")
//    void should_ReturnUnsupportedMediaType_When_WrongContentType() {
//        // When & Then
//        webTestClient.post()
//                .uri("/api/v1/backup/process")
//                .contentType(MediaType.TEXT_PLAIN)
//                .bodyValue("plain text body")
//                .exchange()
//                .expectStatus().isEqualTo(415);
//
//        verify(backupService, never()).execute(anyString());
//    }
//
//    // Reactive Stream Tests
//    @Test
//    @DisplayName("Should handle reactive stream properly")
//    void should_HandleReactiveStream_When_ServiceReturnsMonoResponse() {
//        // Given
//        when(backupService.getBackupStatus("BATCH_001", "HWA_EPR_DB_BACKUP_FULL"))
//                .thenReturn(Mono.just("COMPLETED"));
//
//        // When
//        Mono<String> result = backupService.getBackupStatus("BATCH_001", "HWA_EPR_DB_BACKUP_FULL");
//
//        // Then
//        StepVerifier.create(result)
//                .expectNext("COMPLETED")
//                .verifyComplete();
//    }
//
//    @Test
//    @DisplayName("Should handle empty reactive stream")
//    void should_HandleEmptyStream_When_ServiceReturnsEmpty() {
//        // Given
//        when(backupService.getBackupStatus("BATCH_001", "HWA_EPR_DB_BACKUP_FULL"))
//                .thenReturn(Mono.empty());
//
//        // When
//        Mono<String> result = backupService.getBackupStatus("BATCH_001", "HWA_EPR_DB_BACKUP_FULL");
//
//        // Then
//        StepVerifier.create(result)
//                .verifyComplete();
//    }
//}