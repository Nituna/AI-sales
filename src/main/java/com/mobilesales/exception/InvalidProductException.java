package com.mobilesales.exception;

/**
 * Exception thrown when product data is missing or invalid.
 */
public class InvalidProductException extends Exception {
    public InvalidProductException(String message) {
        super(message);
    }

    public InvalidProductException(String message, Throwable cause) {
        super(message, cause);
    }
}
