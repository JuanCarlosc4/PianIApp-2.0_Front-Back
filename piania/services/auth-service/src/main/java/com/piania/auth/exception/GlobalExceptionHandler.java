package com.piania.auth.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiError> handleCustom(CustomException ex) {

        ApiError error = new ApiError(
                ex.getStatus(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {

        // Log stacktrace to container logs so we can debug 500s
        ex.printStackTrace();

        int status = 500;
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";

        ApiError error = new ApiError(
                status,
                message,
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(error);
    }
}
