package com.petsplatform.shared.response;

import java.time.Instant;

public final class ApiResponseBuilder<T> {

    private boolean success = true;
    private String message = "Success";
    private T data;
    private Instant timestamp = Instant.now();
    private ApiError error;

    /* Package-private — only ApiResponse (same package) can instantiate */
    ApiResponseBuilder() {}

    public static <T> ApiResponseBuilder<T> create() {
        return new ApiResponseBuilder<>();
    }

    public static <T> ApiResponseBuilder<T> forData(T data) {
        ApiResponseBuilder<T> b = new ApiResponseBuilder<>();
        b.setData(data);
        return b;
    }

    public ApiResponseBuilder<T> success(boolean success) {
        this.success = success;
        return this;
    }

    public ApiResponseBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    public ApiResponseBuilder<T> data(T data) {
        this.data = data;
        return this;
    }

    /* Package-private setter — called by ApiResponse.builder(T) */
    void setData(T data) {
        this.data = data;
    }

    public ApiResponseBuilder<T> timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ApiResponseBuilder<T> error(ApiError error) {
        this.success = false;
        this.error = error;
        return this;
    }

    public ApiResponseBuilder<T> error(ApiError error, String overrideMessage) {
        this.success = false;
        this.message = overrideMessage;
        this.error = error;
        return this;
    }

    /** Builds the response. Calls package-private constructor of ApiResponse. */
    public ApiResponse<T> build() {
        return new ApiResponse<>(success, message, data, timestamp, error);
    }
}
