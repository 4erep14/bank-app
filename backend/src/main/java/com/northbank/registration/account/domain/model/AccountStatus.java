// Story: US-006 / US-009
package com.northbank.registration.account.domain.model;

/**
 * Lifecycle states of a bank account.
 *
 * <ul>
 *   <li>{@code ACTIVE}   — open and operational (default on creation)</li>
 *   <li>{@code INACTIVE} — deactivated by admin (AC4); can be reactivated (AC5)</li>
 *   <li>{@code CLOSED}   — permanently closed (out of scope for current stories)</li>
 *   <li>{@code FROZEN}   — temporarily frozen, e.g. fraud hold</li>
 * </ul>
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    CLOSED,
    FROZEN
}
