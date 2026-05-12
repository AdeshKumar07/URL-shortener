package com.devops.urlshortener;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts unhandled exceptions into clean JSON error responses.
 *
 * Without this, Spring Boot returns its white-label HTML error page for
 * bad requests (e.g. malformed JSON body), which the UI cannot parse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Catches malformed or missing JSON request bodies and returns a
     * structured { "error": "..." } response instead of a 400 HTML page.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleBadJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Request body is missing or not valid JSON"));
    }

    /**
     * Spring MVC can raise these when no controller/static resource matches.
     * We must not convert them into 500s, otherwise the UI/tests see a server error.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(Exception ex) {
        return ResponseEntity.status(404)
                .body(Map.of("error", "Not found"));
    }

    /**
     * Safety net: any other unexpected exception returns 500 JSON
     * instead of the default HTML error page.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred. Please try again."));
    }
}
