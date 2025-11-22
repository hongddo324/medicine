package com.medicine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockSearchException.class)
    public ResponseEntity<ErrorResponse> handleStockSearchException(StockSearchException e) {
        log.warn("Stock search exception: {}", e.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .reason("STOCK_SEARCH_FAILED")
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected exception", e);
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .reason("INTERNAL_ERROR")
                .message("서버 내부 오류가 발생했습니다")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
