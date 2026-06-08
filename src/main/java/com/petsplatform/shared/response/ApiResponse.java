package com.petsplatform.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.function.Supplier;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Instant timestamp;
    private final ApiError error;

    /* Package-private — accessible within same package (ApiResponseBuilder) */
    ApiResponse(boolean success, String message, T data, Instant timestamp, ApiError error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.error = error;
    }

    /* ── Getters (for JSON serialization) ─────────────────────── */

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public ApiError getError() {
        return error;
    }

    /* ── Static factories: Success ──────────────────────────── */

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, "Success", null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> okMsg(String message) {
        return new ApiResponse<>(true, message, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> okMsgData(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), null);
    }

    /* ── Static factories: Error ────────────────────────────── */

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> errorMsg(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, Instant.now(), error);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(false, error.getMessage(), null, Instant.now(), error);
    }

    public static <T> ApiResponse<T> error(ApiError error, String overrideMessage) {
        return new ApiResponse<>(false, overrideMessage, null, Instant.now(), error);
    }

    /* ── Static factories: Utility ──────────────────────────── */

    public static <T> ApiResponse<T> of(Supplier<T> supplier) {
        try {
            return ok(supplier.get());
        } catch (Exception e) {
            return error("Operation failed: " + e.getMessage());
        }
    }

    public static <T> ApiResponse<T> of(T value, boolean ok) {
        return ok ? ok(value) : error("Operation failed");
    }

    /* ── Builder ─────────────────────────────────────────────── */

    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static <T> ApiResponseBuilder<T> builder(T data) {
        ApiResponseBuilder<T> b = new ApiResponseBuilder<>();
        b.setData(data);
        return b;
    }
}
