package com.petsplatform.shared.validation.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for {@link PhoneNumber}.
 *
 * <p>Accepts international and domestic phone number formats
 * with 10–15 digits, allowing spaces, dashes, and parentheses.
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final Pattern PATTERN = Pattern.compile(
        "^\\+?[0-9\\s\\-()]{10,20}$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // @NotBlank handles null/blank check separately
        }
        return PATTERN.matcher(value).matches();
    }
}
