package com.petsplatform.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Generic API response wrapper - đảm bảo frontend luôn nhận được response
 * có cấu trúc nhất quán.
 *
 * @param <T> Kiểu dữ liệu của payload
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String timestamp;

    private ApiResponse(boolean success, String message, T data, String timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    /**
     * Tạo response thành công với data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * Tạo response thành công với custom message và data.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * Tạo response thành công chỉ với message (không có data).
     */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .data(null)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * Tạo response lỗi với message.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * Tạo response lỗi với ApiError.
     */
    public static <T> ApiResponse<T> error(ApiError apiError) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(apiError.getMessage())
                .data(null)
                .timestamp(Instant.now().toString())
                .build();
    }
}
