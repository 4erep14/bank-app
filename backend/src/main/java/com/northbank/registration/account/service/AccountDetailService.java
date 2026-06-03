// Story: US-008
package com.northbank.registration.account.service;

import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.exception.AccountAccessDeniedException;
import com.northbank.registration.account.exception.AccountNotFoundException;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.account.service.dto.AccountDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.UUID;

/**
 * Business logic for US-008: View Account Details.
 *
 * <p>Security model: ownership check is performed <em>before</em> returning
 * any account data to avoid leaking existence information via timing.
 * We intentionally return 404 for a non-existent ID and 403 for a
 * different customer's account — a found-but-forbidden account is not
 * treated as 404 here because the customer is already authenticated and
 * the account ID is from their own navigation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDetailService {

    private final AccountRepository accountRepository;

    /**
     * Returns full details of a single account.
     *
     * @param customerId authenticated customer UUID (extracted from JWT at controller layer)
     * @param accountId  the account UUID requested
     * @return account detail view
     * @throws AccountNotFoundException     if no account with {@code accountId} exists (AC4 → 404)
     * @throws AccountAccessDeniedException if the account belongs to a different customer (AC3 → 403)
     */
    @Transactional(readOnly = true)
    public AccountDetailResponse getAccountDetail(UUID customerId, UUID accountId) {
        log.debug("Account detail request: accountId={}, customerId={}", accountId, customerId);

        BankAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // AC3: ownership check — must happen after the existence check
        if (!account.getCustomerId().equals(customerId)) {
            log.warn("Ownership violation: customerId={} attempted access to accountId={}",
                    customerId, accountId);
            throw new AccountAccessDeniedException();
        }

        return toDetail(account);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private AccountDetailResponse toDetail(BankAccount account) {
        return new AccountDetailResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance().setScale(2, RoundingMode.HALF_UP),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
