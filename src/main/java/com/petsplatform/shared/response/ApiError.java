package com.petsplatform.shared.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Immutable error response wrapper.
 * Dùng để trả về chi tiết lỗi có cấu trúc nhất quán.
 */
@Getter
@Builder
public final class ApiError {

    private final String code;
    private final String message;
    private final String details;

    private ApiError(String code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public static ApiError of(String code, String message) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .build();
    }

    public static ApiError of(String code, String message, String details) {
        return ApiError.builder()
                .code(code)
                .message(message)
                .details(details)
                .build();
    }
}
