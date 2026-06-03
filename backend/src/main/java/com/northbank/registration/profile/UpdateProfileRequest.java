// Story: US-005
package com.northbank.registration.profile;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.northbank.registration.profile.exception.FieldNotEditableException;
import com.northbank.registration.validation.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for {@code PATCH /api/v1/profile} — partial profile update.
 *
 * <p>All three mutable fields are individually optional. Only non-null fields
 * are applied to the entity in the service layer — a PATCH body that omits a
 * field leaves that field unchanged (AC2, AC5).</p>
 *
 * <h2>Forbidden-field enforcement (AC3)</h2>
 * Jackson calls {@link #rejectReadOnlyField(String, Object)} for every JSON
 * property that does not map to a declared setter. This sentinel method
 * immediately throws {@link FieldNotEditableException}, which the
 * {@code GlobalExceptionHandler} maps to HTTP 400 with detail
 * {@code "Field is not editable"}.</p>
 *
 * <h2>Validation notes</h2>
 * <ul>
 *   <li>{@code @Size(min=1, max=100)} on name fields: null is accepted (partial
 *       update), empty string is rejected (min=1). {@code @NotBlank} is
 *       intentionally <em>not</em> used because it fails on null, which would
 *       break partial-update semantics.</li>
 *   <li>{@code @PhoneNumber} accepts null (field omitted) and validates E.164
 *       format when a value is provided (AC4).</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Schema(description = "New first name (optional)", example = "Ada")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Schema(description = "New last name (optional)", example = "Byron")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @Schema(description = "New phone number in E.164 format (optional)", example = "+14155559999")
    @PhoneNumber
    private String phoneNumber;

    /**
     * Jackson sentinel for unknown JSON properties (AC3 enforcement).
     *
     * <p>If a client sends {@code "email"} or {@code "dateOfBirth"} (or any other
     * unrecognised field) in the request body, Jackson routes them here and this
     * method immediately throws {@link FieldNotEditableException}.
     * Spring Boot's default Jackson configuration has
     * {@code DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false}, so
     * without this sentinel the forbidden fields would be silently ignored.</p>
     *
     * @param name  the JSON property name
     * @param value the JSON property value (unused)
     * @throws FieldNotEditableException always — no unknown property is permitted
     */
    @JsonAnySetter
    public void rejectReadOnlyField(String name, Object value) {
        throw new FieldNotEditableException(name);
    }
}
