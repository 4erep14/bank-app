// Story: US-001
package com.northbank.registration.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom Bean Validation annotation enforcing the password complexity rules
 * specified in AC3:
 * <ul>
 *   <li>At least 8 characters</li>
 *   <li>At least one uppercase letter (A–Z)</li>
 *   <li>At least one lowercase letter (a–z)</li>
 *   <li>At least one digit (0–9)</li>
 *   <li>At least one special character (!@#$%^&amp;* …)</li>
 * </ul>
 *
 * <p>Violations produce a single, human-readable message that lists every
 * failing requirement so the user knows exactly what to fix.</p>
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {

    String message() default "Password does not meet complexity requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
