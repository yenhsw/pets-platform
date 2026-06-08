package com.petsplatform.shared.response;

import org.springframework.http.HttpStatus;

public enum ApiError {

    // 4xx Client errors
    BAD_REQUEST("ERR_400", "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("ERR_401", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_403", "Access denied", HttpStatus.FORBIDDEN),
    NOT_FOUND("ERR_404", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("ERR_409", "Resource conflict", HttpStatus.CONFLICT),
    VALIDATION_FAILED("ERR_422", "Validation failed", HttpStatus.UNPROCESSABLE_ENTITY),
    TOO_MANY_REQUESTS("ERR_429", "Too many requests", HttpStatus.TOO_MANY_REQUESTS),

    // 5xx Server errors
    INTERNAL_ERROR("ERR_500", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("ERR_503", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GATEWAY_TIMEOUT("ERR_504", "Gateway timeout", HttpStatus.GATEWAY_TIMEOUT),

    // Application-specific errors (4xx by default)
    RESOURCE_ALREADY_EXISTS("ERR_1001", "Resource already exists", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND("ERR_1002", "Resource not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("ERR_1003", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("ERR_1004", "Token expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("ERR_1005", "Token invalid", HttpStatus.UNAUTHORIZED),
    OPERATION_NOT_ALLOWED("ERR_1006", "Operation not allowed", HttpStatus.FORBIDDEN),
    DATA_INTEGRITY_VIOLATION("ERR_1007", "Data integrity violation", HttpStatus.CONFLICT),
    DATABASE_ERROR("ERR_2001", "Database error", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("ERR_3001", "External service error", HttpStatus.BAD_GATEWAY),
    CACHE_ERROR("ERR_4001", "Cache error", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ApiError(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
