// Story: US-001 / US-002
package com.northbank.registration.customer.domain.model;

/**
 * Lifecycle status of a Customer record.
 * <ul>
 *   <li>{@code PENDING_VERIFICATION} — set by US-001 on registration.</li>
 *   <li>{@code ACTIVE} — set after email/OTP verification.</li>
 *   <li>{@code SUSPENDED} — administratively suspended.</li>
 *   <li>{@code CLOSED} — permanently closed.</li>
 *   <li>{@code LOCKED} — locked after 5 consecutive failed login attempts (US-002, AC4/AC5).</li>
 * </ul>
 */
public enum CustomerStatus {

    /** Registered but awaiting email verification (initial state — AC5 US-001). */
    PENDING_VERIFICATION,

    /** Email verified and account fully active. */
    ACTIVE,

    /** Account temporarily suspended by an administrator. */
    SUSPENDED,

    /** Account permanently closed. */
    CLOSED,

    /**
     * Account locked due to 5 consecutive failed login attempts (US-002 AC4).
     * Can only be unlocked by a Bank Admin (US-002 AC5).
     */
    LOCKED
}
