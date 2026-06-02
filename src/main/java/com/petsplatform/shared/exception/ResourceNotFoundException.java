package com.petsplatform.shared.exception;

import com.petsplatform.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception được ném ra khi không tìm thấy resource.
 * Tự động map sang HTTP 404.
 */
@Getter
public final class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            ErrorCode.RESOURCE_NOT_FOUND,
            buildMessage(resourceName, fieldName, fieldValue)
        );
    }

    private static String buildMessage(String resourceName, String fieldName, Object fieldValue) {
        return String.format("Không tìm thấy %s với %s: '%s'", resourceName, fieldName, fieldValue);
    }
}
