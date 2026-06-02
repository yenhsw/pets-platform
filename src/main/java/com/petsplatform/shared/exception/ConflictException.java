package com.petsplatform.shared.exception;

import com.petsplatform.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception được ném ra khi có xung đột dữ liệu.
 * Ví dụ: sửa đổi đồng thời, lỗi optimistic lock.
 */
@Getter
public final class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.UNKNOWN_ERROR, message);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
