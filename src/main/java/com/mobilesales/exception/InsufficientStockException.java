package com.mobilesales.exception;

/**
 * Exception thrown when stock levels are critically insufficient for orders or operations.
 */
public class InsufficientStockException extends Exception {
    private final int requested;
    private final int available;

    public InsufficientStockException(String message, int requested, int available) {
        super(message + " [Requested: " + requested + ", Available: " + available + "]");
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
