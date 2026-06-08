package com.petsplatform.shared.exception;

import com.petsplatform.shared.response.ApiError;
import com.petsplatform.shared.response.ApiResponse;
import com.petsplatform.shared.response.ApiResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST endpoints.
 * Uses ApiResponse.error(...) to maintain consistent JSON structure across all APIs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ══════════════════════════════════════════════════════════
       1. BUSINESS EXCEPTIONS
       ══════════════════════════════════════════════════════════ */

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(ex.getError(), ex.getDisplayMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ResourceConflictException ex) {
        return buildResponse(ex.getError(), ex.getDisplayMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(ValidationException ex) {
        ApiResponse<Map<String, String>> response = ApiResponse
            .<Map<String, String>>builder()
            .success(false)
            .message(ex.getDisplayMessage())
            .data(ex.getFieldErrors())
            .error(ex.getError())
            .build();
        return ResponseEntity.status(ex.getError().getHttpStatus()).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return buildResponse(ex.getError(), ex.getDisplayMessage());
    }

    /* ══════════════════════════════════════════════════════════
       2. SPRING VALIDATION EXCEPTIONS
       ══════════════════════════════════════════════════════════ */

    /**
     * Handles @Valid on @RequestBody DTOs.
     * Example: @PostMapping + @RequestBody + @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                (a, b) -> a
            ));
        return buildValidationResponse(fieldErrors);
    }

    /**
     * Handles @Validated on @RequestParam / @PathVariable method arguments.
     * Example: @Validated + @RequestParam
     */
    @SuppressWarnings("deprecation")
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (var result : ex.getAllValidationResults()) {
            for (var err : result.getResolvableErrors()) {
                String field = err.getCodes() != null && err.getCodes().length > 0
                    ? err.getCodes()[err.getCodes().length - 1] : "unknown";
                String msg = err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value";
                fieldErrors.putIfAbsent(field, msg);
            }
        }
        return buildValidationResponse(fieldErrors);
    }

    /**
     * Handles JSR-303 @Constraint on method parameters.
     * Example: @Validated + @NotNull on @RequestParam
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {
        Map<String, String> fieldErrors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(),
                v -> v.getMessage(),
                (a, b) -> a
            ));
        return buildValidationResponse(fieldErrors);
    }

    /* ══════════════════════════════════════════════════════════
       3. HTTP REQUEST EXCEPTIONS
       ══════════════════════════════════════════════════════════ */

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildResponse(ApiError.BAD_REQUEST,
            "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' should be of type %s",
            ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return buildResponse(ApiError.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return buildResponse(ApiError.BAD_REQUEST, "Malformed JSON request body");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return buildResponse(ApiError.NOT_FOUND, "API endpoint not found");
    }

    /* ══════════════════════════════════════════════════════════
       4. CATCH-ALL (LAST)
       ══════════════════════════════════════════════════════════ */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        // Log the full stack trace for debugging
        // In production, use a proper logger and consider masking sensitive info
        ex.printStackTrace();
        return buildResponse(ApiError.INTERNAL_ERROR, "An unexpected error occurred");
    }

    /* ══════════════════════════════════════════════════════════
       HELPER METHODS
       ══════════════════════════════════════════════════════════ */

    private ResponseEntity<ApiResponse<Void>> buildResponse(ApiError error, String message) {
        return ResponseEntity
            .status(error.getHttpStatus())
            .body(ApiResponse.error(error, message));
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> buildValidationResponse(
            Map<String, String> fieldErrors) {
        String message = "Validation failed for " + fieldErrors.size() + " field(s)";
        ApiResponse<Map<String, String>> response = ApiResponse
            .<Map<String, String>>builder()
            .success(false)
            .message(message)
            .data(fieldErrors)
            .error(ApiError.VALIDATION_FAILED)
            .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }
}
