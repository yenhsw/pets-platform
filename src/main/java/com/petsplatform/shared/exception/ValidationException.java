package com.petsplatform.shared.exception;

import com.petsplatform.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception được ném ra khi validation thất bại.
 * Giữ thông tin lỗi theo từng field.
 */
@Getter
public final class ValidationException extends BusinessException {

    private final java.util.Map<String, String> errors;

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.errors = new java.util.HashMap<>();
    }

    public ValidationException(java.util.Map<String, String> errors) {
        super(ErrorCode.VALIDATION_ERROR, "Xác thực thất bại");
        this.errors = errors != null ? new java.util.HashMap<>(errors) : new java.util.HashMap<>();
    }

    public ValidationException addFieldError(String field, String message) {
        this.errors.put(field, message);
        return this;
    }
}
