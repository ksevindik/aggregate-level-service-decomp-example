package com.example.clubservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = { RuntimeException.class }) // Catch all RuntimeExceptions
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        // Example response structure
        String bodyOfResponse = "Operation failed: " + ex.getMessage() + ". Please try again later.";
        // Log the exception details
        // You could also map different exceptions to different status codes or response bodies
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(bodyOfResponse);
    }

    // You can add more exception handlers here for specific exceptions
}