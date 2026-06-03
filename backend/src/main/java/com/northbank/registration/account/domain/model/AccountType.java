// Story: US-006
package com.northbank.registration.account.domain.model;

/**
 * Supported bank account types for US-006.
 * A customer may hold at most one account of each type (AC5).
 */
public enum AccountType {
    CHECKING,
    SAVINGS
}
