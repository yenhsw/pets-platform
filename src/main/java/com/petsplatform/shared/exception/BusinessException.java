package com.petsplatform.shared.exception;

import com.petsplatform.shared.response.ApiError;

/**
 * Base unchecked exception for all business rule violations.
 * Carries an ApiError code so GlobalExceptionHandler can map it to HTTP status.
 */
public class BusinessException extends RuntimeException {

    private final ApiError error;
    private final String overrideMessage;

    public BusinessException(ApiError error) {
        super(error.getMessage());
        this.error = error;
        this.overrideMessage = null;
    }

    public BusinessException(ApiError error, String overrideMessage) {
        super(overrideMessage);
        this.error = error;
        this.overrideMessage = overrideMessage;
    }

    public ApiError getError() {
        return error;
    }

    public String getDisplayMessage() {
        return overrideMessage != null ? overrideMessage : error.getMessage();
    }
}
