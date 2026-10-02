package com.mobilesales.exception;

/**
 * Exception thrown when validation fails on incoming user inputs or CSV data rows.
 */
public class DataValidationException extends Exception {
    public DataValidationException(String message) {
        super(message);
    }

    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
