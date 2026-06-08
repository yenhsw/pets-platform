package com.petsplatform.shared.validation.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for {@link EnumValue}.
 *
 * <p>Checks if the input string matches the name of any enum constant
 * in the target enum class (case-insensitive by default).
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Set<String> validValues;

    @Override
    public void initialize(EnumValue annotation) {
        validValues = Arrays.stream(annotation.value().getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // @NotBlank handles null/blank check separately
        }
        return validValues.contains(value.toUpperCase());
    }
}
