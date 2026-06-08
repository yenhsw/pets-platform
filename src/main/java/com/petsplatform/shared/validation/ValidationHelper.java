package com.petsplatform.shared.validation;

import com.petsplatform.shared.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for manual (service-layer) validation of objects and DTOs.
 *
 * <p>All methods throw {@link ValidationException} on validation failure,
 * which is automatically handled by {@link com.petsplatform.shared.exception.GlobalExceptionHandler}
 * and returned to the client as an HTTP 422 response.
 *
 * <p>For controller-level validation, prefer:
 * <ul>
 *   <li>{@code @Valid} + {@code @RequestBody} for DTO body validation</li>
 *   <li>{@code @Validated} + {@code @RequestParam} for parameter validation</li>
 * </ul>
 *
 * <p>Use this helper when:
 * <ul>
 *   <li>Validating cross-field conditions (e.g., {@code startDate < endDate})</li>
 *   <li>Validating against database state (e.g., "email already exists")</li>
 *   <li>Validating complex object graphs that cannot be expressed in annotations</li>
 *   <li>Validating in service layer before persistence</li>
 * </ul>
 *
 * @see com.petsplatform.shared.exception.ValidationException
 * @see com.petsplatform.shared.exception.GlobalExceptionHandler
 */
@Component
public class ValidationHelper {

    private final Validator validator;

    public ValidationHelper(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates an object and throws {@link ValidationException} if any constraint is violated.
     *
     * @param object the object to validate
     * @param <T>   the type of the object
     * @throws ValidationException if validation fails
     */
    public <T> void validate(T object) {
        validateWithMessage(object, "Validation failed");
    }

    /**
     * Validates an object and throws {@link ValidationException} with a custom message.
     *
     * @param object  the object to validate
     * @param message override message for the exception
     * @param <T>    the type of the object
     * @throws ValidationException if validation fails
     */
    public <T> void validateWithMessage(T object, String message) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            Map<String, String> fieldErrors = toFieldErrors(violations);
            throw new ValidationException(fieldErrors);
        }
    }

    /**
     * Validates an object and returns field errors as a map.
     * Does NOT throw — caller decides how to handle.
     *
     * @param object the object to validate
     * @param <T>   the type of the object
     * @return a map of field -> error message (empty if valid)
     */
    public <T> Map<String, String> validateAndGetErrors(T object) {
        return toFieldErrors(validator.validate(object));
    }

    /**
     * Checks if an object is valid without throwing.
     *
     * @param object the object to validate
     * @param <T>   the type of the object
     * @return true if valid, false otherwise
     */
    public <T> boolean isValid(T object) {
        return validator.validate(object).isEmpty();
    }

    /**
     * Throws {@link ValidationException} if the condition is false.
     * Useful for simple boolean checks.
     *
     * @param condition the condition that must be true
     * @param field    the field name for the error message
     * @param message  the error message if condition is false
     * @throws ValidationException if condition is false
     */
    public void requireTrue(boolean condition, String field, String message) {
        if (!condition) {
            throw new ValidationException(field, message);
        }
    }

    /**
     * Throws {@link ValidationException} if the value is null.
     *
     * @param value  the value to check
     * @param field the field name for the error message
     * @throws ValidationException if value is null
     */
    public void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException(field, "Field is required");
        }
    }

    /**
     * Throws {@link ValidationException} with field errors from a map.
     *
     * @param fieldErrors map of field -> error message
     * @throws ValidationException always (unless map is empty)
     */
    public void requireValid(Map<String, String> fieldErrors) {
        if (!fieldErrors.isEmpty()) {
            throw new ValidationException(fieldErrors);
        }
    }

    /**
     * Converts a set of constraint violations to a field → message map.
     */
    private <T> Map<String, String> toFieldErrors(Set<ConstraintViolation<T>> violations) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<T> v : violations) {
            String field = v.getPropertyPath().toString();
            // For nested paths like "address.street", take the last segment
            if (field.contains(".")) {
                field = field.substring(field.lastIndexOf('.') + 1);
            }
            fieldErrors.putIfAbsent(field, v.getMessage());
        }
        return fieldErrors;
    }
}
