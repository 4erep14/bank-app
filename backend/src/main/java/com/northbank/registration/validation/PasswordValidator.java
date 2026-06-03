// Story: US-001
package com.northbank.registration.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that a password string satisfies all AC3 complexity requirements:
 * <ul>
 *   <li>≥ 8 characters</li>
 *   <li>≥ 1 uppercase letter</li>
 *   <li>≥ 1 lowercase letter</li>
 *   <li>≥ 1 digit</li>
 *   <li>≥ 1 special character</li>
 * </ul>
 *
 * <p>When the value is {@code null} or blank, validation is delegated to
 * {@code @NotBlank} — this validator returns {@code true} for null to avoid
 * double-reporting.</p>
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~\\";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // @NotBlank handles the null/blank case; avoid duplicate messages
            return true;
        }

        List<String> violations = new ArrayList<>();

        if (value.length() < 8) {
            violations.add("at least 8 characters");
        }
        if (!containsUppercase(value)) {
            violations.add("at least one uppercase letter");
        }
        if (!containsLowercase(value)) {
            violations.add("at least one lowercase letter");
        }
        if (!containsDigit(value)) {
            violations.add("at least one digit");
        }
        if (!containsSpecialChar(value)) {
            violations.add("at least one special character");
        }

        if (violations.isEmpty()) {
            return true;
        }

        // Build a clear, actionable message listing every missing requirement
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "Password must contain: " + String.join(", ", violations)
        ).addConstraintViolation();

        return false;
    }

    private boolean containsUppercase(String s) {
        return s.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowercase(String s) {
        return s.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String s) {
        return s.chars().anyMatch(Character::isDigit);
    }

    private boolean containsSpecialChar(String s) {
        for (char c : s.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
