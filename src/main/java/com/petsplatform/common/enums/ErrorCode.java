package com.petsplatform.common.enums;

/**
 * Các mã lỗi chuẩn hóa cho toàn bộ hệ thống.
 * Format: DOMAIN_MA_LOI (ví dụ: PET_NOT_FOUND, USER_INVALID_INPUT)
 */
public enum ErrorCode {

    // ========== Lỗi chung (1xxx) ==========
    UNKNOWN_ERROR("SYS_0001", "An unknown error occurred"),
    VALIDATION_ERROR("SYS_0002", "Validation failed"),
    INVALID_REQUEST("SYS_0003", "Invalid request"),

    // ========== Lỗi tài nguyên (2xxx) ==========
    RESOURCE_NOT_FOUND("RES_2001", "Resource not found"),
    RESOURCE_ALREADY_EXISTS("RES_2002", "Resource already exists"),

    // ========== Lỗi người dùng (3xxx) ==========
    USER_NOT_FOUND("USR_3001", "User not found"),
    USER_ALREADY_EXISTS("USR_3002", "User already exists"),
    USER_INVALID_CREDENTIALS("USR_3003", "Invalid credentials"),
    USER_ACCOUNT_LOCKED("USR_3004", "Account is locked"),
    USER_NOT_ACTIVE("USR_3005", "User account is not active"),

    // ========== Lỗi thú cưng (4xxx) ==========
    PET_NOT_FOUND("PET_4001", "Pet not found"),
    PET_ALREADY_EXISTS("PET_4002", "Pet already exists"),
    PET_INVALID_STATUS("PET_4003", "Invalid pet status"),

    // ========== Lỗi trại tạm (5xxx) ==========
    SHELTER_NOT_FOUND("SHL_5001", "Shelter not found"),
    SHELTER_FULL("SHL_5002", "Shelter is at full capacity"),

    // ========== Lỗi nhận nuôi (6xxx) ==========
    ADOPTION_NOT_FOUND("ADP_6001", "Adoption request not found"),
    ADOPTION_ALREADY_PROCESSED("ADP_6002", "Adoption request already processed"),
    ADOPTION_INVALID_STATUS("ADP_6003", "Invalid adoption status"),

    // ========== Lỗi xác thực (9xxx) ==========
    FIELD_REQUIRED("VAL_9001", "Field is required"),
    FIELD_INVALID_FORMAT("VAL_9002", "Field has invalid format"),
    FIELD_TOO_SHORT("VAL_9003", "Field is too short"),
    FIELD_TOO_LONG("VAL_9004", "Field is too long");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
