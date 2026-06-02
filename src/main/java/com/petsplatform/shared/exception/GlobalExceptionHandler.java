package com.petsplatform.shared.exception;

import com.petsplatform.common.enums.ErrorCode;
import com.petsplatform.shared.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý exception toàn cục - tập trung xử lý exception.
 * Đảm bảo tất cả exceptions được xử lý nhất quán.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý ResourceNotFoundException → 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Không tìm thấy tài nguyên: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Xử lý ResourceAlreadyExistsException → 409
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(
            ResourceAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Tài nguyên đã tồn tại: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Xử lý ConflictException → 409
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Xung đột dữ liệu: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Xử lý ValidationException → 400
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex,
            HttpServletRequest request
    ) {
        log.warn("Lỗi xác thực: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.withFieldErrors(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex.getErrors(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý Spring Validation (Bean Validation) → 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Bean validation thất bại: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse response = ErrorResponse.withFieldErrors(
                ErrorCode.VALIDATION_ERROR.getCode(),
                "Xác thực thất bại",
                fieldErrors,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý BusinessException → map theo errorCode
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {
        log.warn("Lỗi nghiệp vụ: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                ex.getDetails(),
                request.getRequestURI()
        );

        return ResponseEntity.status(mapErrorCodeToHttpStatus(ex.getErrorCode())).body(response);
    }

    /**
     * Xử lý IllegalArgumentException → 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Tham số không hợp lệ: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý TẤT CẢ exceptions chưa được xử lý → 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Đã xảy ra lỗi không mong muốn", ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.UNKNOWN_ERROR.getCode(),
                "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Map ErrorCode sang HTTP Status.
     */
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case RESOURCE_NOT_FOUND, PET_NOT_FOUND, USER_NOT_FOUND,
                 SHELTER_NOT_FOUND, ADOPTION_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case RESOURCE_ALREADY_EXISTS, USER_ALREADY_EXISTS,
                 PET_ALREADY_EXISTS, ADOPTION_ALREADY_PROCESSED -> HttpStatus.CONFLICT;

            case VALIDATION_ERROR, INVALID_REQUEST, FIELD_REQUIRED,
                 FIELD_INVALID_FORMAT, FIELD_TOO_SHORT, FIELD_TOO_LONG,
                 USER_INVALID_CREDENTIALS, PET_INVALID_STATUS,
                 ADOPTION_INVALID_STATUS -> HttpStatus.BAD_REQUEST;

            case USER_ACCOUNT_LOCKED -> HttpStatus.LOCKED;
            case USER_NOT_ACTIVE -> HttpStatus.FORBIDDEN;

            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
