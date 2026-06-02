package com.petsplatform.shared.exception;

import com.petsplatform.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Exception được ném ra khi resource đã tồn tại.
 * Tự động map sang HTTP 409 Conflict.
 */
@Getter
public final class ResourceAlreadyExistsException extends BusinessException {

    public ResourceAlreadyExistsException(String resourceName, String fieldName, Object fieldValue) {
        super(
            ErrorCode.RESOURCE_ALREADY_EXISTS,
            buildMessage(resourceName, fieldName, fieldValue)
        );
    }

    private static String buildMessage(String resourceName, String fieldName, Object fieldValue) {
        return String.format("%s đã tồn tại với %s: '%s'", resourceName, fieldName, fieldValue);
    }
}
