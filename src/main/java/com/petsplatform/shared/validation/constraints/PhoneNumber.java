package com.petsplatform.shared.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a valid phone number.
 *
 * <p>Accepted formats:
 * <ul>
 *   <li>International: +84 123 456 789, +84-123-456-789</li>
 *   <li>Vietnam domestic: 0912 345 678, 0912-345-678, 01234567890</li>
 *   <li>US format: (123) 456-7890</li>
 *   <li>General: 10–15 digits, spaces/dashes/parentheses allowed</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@code @PhoneNumber}
 * {@code private String phone;}
 * </pre>
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "{com.petsplatform.shared.validation.constraints.PhoneNumber.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
