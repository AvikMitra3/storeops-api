package com.cognizant.storeops.shared.error;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppError.class)
    ResponseEntity<ErrorResponse> app(AppError e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(e.getCode(), e.getMessage(), e.getStatusCode(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(x -> x.getField() + " " + x.getDefaultMessage()).orElse("Invalid request");
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message, 400, Instant.now()));
    }
}
