package com.mobilesales.exception;

/**
 * Exception thrown when sales numbers, dates, or revenue are invalid.
 */
public class InvalidSalesDataException extends Exception {
    public InvalidSalesDataException(String message) {
        super(message);
    }

    public InvalidSalesDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
