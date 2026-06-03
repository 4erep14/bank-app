// Story: US-005
package com.northbank.registration.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator for the {@link PhoneNumber} annotation.
 *
 * <p>Accepts {@code null} values — callers that require a non-null phone number
 * should additionally annotate the field with {@code @NotNull} or {@code @NotBlank}.
 * This design allows reuse on optional PATCH request fields (US-005) without
 * special-casing null.</p>
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    /** ITU-T E.164: {@code +} followed by 2–15 digits, leading digit non-zero. */
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null is considered valid — use @NotNull / @NotBlank to enforce presence
        if (value == null) {
            return true;
        }
        return E164_PATTERN.matcher(value).matches();
    }
}
