// Story: US-005 | US-010
package com.northbank.registration.shared.security;

import java.lang.reflect.Method;
import java.util.UUID;

public final class AuthenticatedCustomer {

    private AuthenticatedCustomer() {
    }

    public static UUID resolveId(Object principal) {
        if (principal instanceof UUID customerId) {
            return customerId;
        }
        if (principal instanceof String subject) {
            return UUID.fromString(subject);
        }
        String subject = readSubject(principal);
        if (subject != null) {
            return UUID.fromString(subject);
        }
        throw new IllegalStateException("Unsupported authentication principal");
    }

    private static String readSubject(Object principal) {
        if (principal == null) {
            return null;
        }
        try {
            Method method = principal.getClass().getMethod("getSubject");
            Object value = method.invoke(principal);
            return value instanceof String subject ? subject : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
