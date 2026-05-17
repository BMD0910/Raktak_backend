package com.raktakk.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = base(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Données invalides", request);
        body.put("errors", errors);
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        String message = "Email ou mot de passe incorrect.";
        if (ex instanceof AuthenticationServiceException) {
            message = "Erreur de service d'authentification, veuillez réessayer.";
        }
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", message, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String lower = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage().toLowerCase()
                : "";
        String message = lower.contains("email")
                ? "Cet email est déjà utilisé."
                : "Conflit de données détecté, vérifiez les informations envoyées.";
        return response(HttpStatus.CONFLICT, "DATA_INTEGRITY_CONFLICT", message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Accès refusé", request);
    }

    @ExceptionHandler({NotVendorException.class, VendorNotVerifiedException.class})
    public ResponseEntity<?> handleVendorAccess(RuntimeException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "VENDOR_ACCESS_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        return response(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElement(java.util.NoSuchElementException ex, HttpServletRequest request) {
        log.warn("Resource not found on {} {}", request.getMethod(), request.getRequestURI());
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Ressource non trouvée", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Une erreur inattendue est survenue. Réessayez dans quelques instants.",
                request
        );
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(base(status, code, message, request));
    }

    private Map<String, Object> base(HttpStatus status, String code, String message, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
