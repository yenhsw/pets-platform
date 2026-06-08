package com.petsplatform.shared.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a strong password.
 *
 * <p>Requirements:
 * <ul>
 *   <li>At least 8 characters</li>
 *   <li>At least 1 uppercase letter</li>
 *   <li>At least 1 lowercase letter</li>
 *   <li>At least 1 digit</li>
 *   <li>At least 1 special character: !@#$%^&*()_+-=[]{}|;:',.<>?/~`</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@code @StrongPassword}
 * {@code private String password;}
 * </pre>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "{com.petsplatform.shared.validation.constraints.StrongPassword.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
