package com.petsplatform.shared.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a String field's value matches one of the values in a target enum.
 *
 * <p>Unlike {@code @Enum({...})} which requires listing values manually,
 * this annotation uses the enum class directly — adding a new enum constant
 * automatically extends validation coverage.
 *
 * <p>Usage:
 * <pre>
 * public enum UserRole { ADMIN, USER, GUEST }
 *
 * {@code @EnumValue(UserRole.class)}
 * {@code private String role;}
 * </pre>
 *
 * <p>Invalid: {@code "admin"}, {@code "MODERATOR"}, {@code ""}
 * <p>Valid: {@code "ADMIN"}, {@code "USER"}, {@code "GUEST"}
 */
@Documented
@Constraint(validatedBy = EnumValueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValue {

    String message() default "{com.petsplatform.shared.validation.constraints.EnumValue.message}";

    Class<? extends Enum<?>> value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
