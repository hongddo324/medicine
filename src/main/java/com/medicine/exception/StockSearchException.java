package com.medicine.exception;

public class StockSearchException extends RuntimeException {
    public StockSearchException(String message) {
        super(message);
    }

    public StockSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
