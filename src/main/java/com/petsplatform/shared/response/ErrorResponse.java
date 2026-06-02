package com.petsplatform.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Cấu trúc response lỗi chuẩn hóa.
 * Immutable - dùng Builder pattern để tạo.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ErrorResponse {

    private final boolean success;
    private final String code;
    private final String message;
    private final String details;
    private final Map<String, String> fieldErrors;
    private final String timestamp;
    private final String path;

    private ErrorResponse(
            boolean success,
            String code,
            String message,
            String details,
            Map<String, String> fieldErrors,
            String timestamp,
            String path
    ) {
        this.success = false;
        this.code = code;
        this.message = message;
        this.details = details;
        this.fieldErrors = fieldErrors;
        this.timestamp = Instant.now().toString();
        this.path = path;
    }

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    public static ErrorResponse of(String code, String message, String details, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .path(path)
                .build();
    }

    public static ErrorResponse withFieldErrors(
            String code,
            String message,
            Map<String, String> fieldErrors,
            String path
    ) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .fieldErrors(fieldErrors)
                .path(path)
                .build();
    }
}
