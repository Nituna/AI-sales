package com.mobilesales.exception;

/**
 * Exception thrown when authentication fails.
 */
public class InvalidLoginException extends Exception {
    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
