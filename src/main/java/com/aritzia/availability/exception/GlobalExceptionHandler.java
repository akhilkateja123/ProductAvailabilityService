package com.aritzia.availability.exception;

import com.aritzia.availability.dto.ErrorResponse;
import com.aritzia.availability.util.SafeText;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // An empty productId (GET /availability/) never reaches the controller; Spring treats it as an
    // unmatched route. Only that path gets the 400 — every other unmatched route is a genuine 404.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.equals("/availability") || path.equals("/availability/")) {
            log.warn("Rejecting request with missing productId path segment");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(400, "Bad Request", "productId must not be null or empty"));
        }
        String safePath = SafeText.of(path);
        log.info("No route for {}", safePath);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", "No route for " + safePath));
    }

    @ExceptionHandler(InvalidProductIdException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProductId(InvalidProductIdException ex) {
        log.warn("Invalid productId rejected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException ex) {
        log.info("Product not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Spring MVC's own exceptions (405, 415, ...) carry their correct status; keep it instead of masking as 500.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            int status = springError.getStatusCode().value();
            log.warn("Request rejected by framework ({}): {}", status, ex.getMessage());
            return ResponseEntity.status(status)
                    .headers(springError.getHeaders())
                    .body(ErrorResponse.of(status, HttpStatus.valueOf(status).getReasonPhrase(),
                            springError.getBody().getDetail()));
        }
        log.error("Unexpected server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
