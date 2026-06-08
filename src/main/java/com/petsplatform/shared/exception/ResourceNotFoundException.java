package com.petsplatform.shared.exception;

/**
 * Thrown when a requested resource does not exist.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName) {
        super(com.petsplatform.shared.response.ApiError.NOT_FOUND,
              resourceName + " not found");
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(com.petsplatform.shared.response.ApiError.NOT_FOUND,
              resourceName + " with id [" + id + "] not found");
    }

    public ResourceNotFoundException(String resourceName, String field, Object value) {
        super(com.petsplatform.shared.response.ApiError.NOT_FOUND,
              resourceName + " with " + field + " [" + value + "] not found");
    }
}
