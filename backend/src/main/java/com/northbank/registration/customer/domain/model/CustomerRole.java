// Story: US-009
package com.northbank.registration.customer.domain.model;

/**
 * Application-level roles for customers.
 *
 * <ul>
 *   <li>{@code CUSTOMER}      — regular authenticated customer (default)</li>
 *   <li>{@code ADMIN}         — bank administrator with access to {@code /api/v1/admin/**}</li>
 *   <li>{@code FRAUD_ANALYST} — analyst with access to {@code /api/v1/fraud/**}</li>
 * </ul>
 */
public enum CustomerRole {
    CUSTOMER,
    ADMIN,
    FRAUD_ANALYST
}
