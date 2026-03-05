package com.scb.backup.exception;

import lombok.RequiredArgsConstructor;

/**
 * DbBackupException - Custom runtime exception for database backup operations.
 *
 * This exception is thrown when backup operations fail due to technical errors
 * such as network issues, API failures, database connection problems, or
 * configuration errors.
 *
 * <p>The exception is handled by {@link GlobalExceptionHandler} which converts
 * it to an appropriate HTTP response with batch execution status.</p>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see GlobalExceptionHandler
 */
@RequiredArgsConstructor
public class DbBackupException extends RuntimeException{

    /**
     * Constructs a new DbBackupException with the specified detail message and cause.
     *
     * @param message The detail message explaining the reason for the exception
     * @param cause The underlying cause of the exception
     */
    public DbBackupException(String message, Throwable cause) {
        super(message, cause);
    }


}
