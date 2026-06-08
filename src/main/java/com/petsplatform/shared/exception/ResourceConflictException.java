package com.petsplatform.shared.exception;

/**
 * Thrown when a resource already exists and the operation requires it to be unique.
 */
public class ResourceConflictException extends BusinessException {

    public ResourceConflictException(String resourceName) {
        super(com.petsplatform.shared.response.ApiError.CONFLICT,
              resourceName + " already exists");
    }

    public ResourceConflictException(String resourceName, String field, Object value) {
        super(com.petsplatform.shared.response.ApiError.CONFLICT,
              resourceName + " with " + field + " [" + value + "] already exists");
    }
}
