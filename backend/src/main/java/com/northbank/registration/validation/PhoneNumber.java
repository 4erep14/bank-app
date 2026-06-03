// Story: US-005
package com.northbank.registration.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Bean Validation annotation enforcing the E.164 phone number format.
 *
 * <p>Pattern: {@code ^\+[1-9]\d{1,14}$} — a leading {@code +}, a non-zero
 * country code digit, and 1–14 more digits (max 15 digits total, per ITU-T E.164).</p>
 *
 * <p>A {@code null} value is considered <strong>valid</strong> so that this
 * annotation may be used on optional PATCH fields without breaking partial-update
 * semantics (US-005). Combine with {@code @NotNull} / {@code @NotBlank} when the
 * field is mandatory (e.g. the registration request in US-001).</p>
 *
 * @see PhoneNumberValidator
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "Phone number must be in E.164 format (e.g. +14155552671)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
