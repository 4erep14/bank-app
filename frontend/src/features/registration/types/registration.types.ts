// Story: US-001

/** Request body sent to POST /api/v1/customers */
export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  dateOfBirth: string;
  password: string;
}

/** Success response body */
export interface RegisterResponse {
  id: string;
}

/** Single field-level error returned by the API (400) */
export interface FieldError {
  field: string;
  message: string;
}

/** API 400 error response shape */
export interface ApiValidationError {
  errors: FieldError[];
}

/** API 409 error response shape */
export interface ApiConflictError {
  detail: string;
}

/** Router state passed to the success screen */
export interface RegistrationSuccessState {
  firstName: string;
}
