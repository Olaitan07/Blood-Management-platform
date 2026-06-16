package com.blood.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(int current, int reserved) {
        super("Cannot reduce stock below reserved units. Current: " + current + ", Reserved: " + reserved);
    }
}
