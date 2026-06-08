package com.petsplatform.shared.exception;

import com.petsplatform.shared.response.ApiError;

import java.util.Map;

/**
 * Thrown when business validation fails (distinct from JSR-303 bean validation).
 * Holds a map of field -> error message for detailed feedback.
 */
public class ValidationException extends BusinessException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(ApiError.VALIDATION_FAILED, message);
        this.fieldErrors = Map.of();
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super(ApiError.VALIDATION_FAILED,
              "Validation failed for " + fieldErrors.size() + " field(s)");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ValidationException(String field, String message) {
        super(ApiError.VALIDATION_FAILED, message);
        this.fieldErrors = Map.of(field, message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
