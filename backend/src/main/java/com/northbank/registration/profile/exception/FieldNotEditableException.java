// Story: US-005
package com.northbank.registration.profile.exception;

/**
 * Thrown when a client attempts to update a read-only field
 * ({@code email} or {@code dateOfBirth}) on the profile PATCH endpoint (AC3).
 *
 * <p>Mapped to HTTP 400 "Field is not editable" by
 * {@link com.northbank.registration.shared.exception.GlobalExceptionHandler}.</p>
 */
public class FieldNotEditableException extends RuntimeException {

    private final String fieldName;

    /**
     * @param fieldName the name of the field that cannot be edited
     */
    public FieldNotEditableException(String fieldName) {
        super("Field is not editable: " + fieldName);
        this.fieldName = fieldName;
    }

    /** Returns the name of the read-only field that triggered this exception. */
    public String getFieldName() {
        return fieldName;
    }
}
