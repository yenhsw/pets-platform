package com.petsplatform.shared.response;

import java.util.Collection;

public final class ResponseHelper {

    private ResponseHelper() {}

    /* ── Success shortcuts ─────────────────────────────────────── */

    public static <T> ApiResponse<T> success() {
        return ApiResponse.ok();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.okMsg(message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.ok(data);
    }

    public static <T> ApiResponse<T> successMsgData(String message, T data) {
        return ApiResponse.okMsgData(message, data);
    }

    public static <T> ApiResponse<Collection<T>> successCollection(Collection<T> data) {
        return ApiResponse.ok(data);
    }

    public static <T> ApiResponse<Collection<T>> successCollectionMsgData(String message, Collection<T> data) {
        return ApiResponse.okMsgData(message, data);
    }

    public static <T> ApiResponse<T> successOptional(java.util.Optional<T> data) {
        return ApiResponse.ok(data.orElse(null));
    }

    public static <T> ApiResponse<T> successOptionalMsgData(String message, java.util.Optional<T> data) {
        return ApiResponse.okMsgData(message, data.orElse(null));
    }

    /* ── Error shortcuts ─────────────────────────────────────── */

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.error(message);
    }

    public static <T> ApiResponse<T> error(ApiError apiError) {
        return ApiResponse.error(apiError);
    }

    public static <T> ApiResponse<T> error(ApiError apiError, String overrideMessage) {
        return ApiResponse.error(apiError, overrideMessage);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return ApiResponse.errorMsg(message, ApiError.BAD_REQUEST);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return ApiResponse.errorMsg(message, ApiError.NOT_FOUND);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return ApiResponse.errorMsg(message, ApiError.UNAUTHORIZED);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return ApiResponse.errorMsg(message, ApiError.FORBIDDEN);
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return ApiResponse.errorMsg(message, ApiError.CONFLICT);
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return ApiResponse.errorMsg(message, ApiError.INTERNAL_ERROR);
    }

    public static <T> ApiResponse<T> serverError() {
        return ApiResponse.error(ApiError.INTERNAL_ERROR);
    }

    /* ── Null-safe factories ────────────────────────────────── */

    public static <T> ApiResponse<T> okOrNotFound(T data, String resourceName) {
        if (data == null) {
            return notFound(resourceName + " not found");
        }
        return success(data);
    }

    public static <T> ApiResponse<T> createdOrConflict(T data, boolean exists, String resourceName) {
        if (exists) {
            return conflict(resourceName + " already exists");
        }
        return success(data);
    }

    /* ── Result wrapper ──────────────────────────────────────── */

    public static <T> ApiResponse<T> of(T value) {
        if (value == null) {
            return notFound("Resource not found");
        }
        return success(value);
    }

    public static <T> ApiResponse<T> of(T value, String notFoundMessage) {
        if (value == null) {
            return notFound(notFoundMessage);
        }
        return success(value);
    }
}
