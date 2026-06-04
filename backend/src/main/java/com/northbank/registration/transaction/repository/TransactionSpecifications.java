// Story: US-011 | US-013
package com.northbank.registration.transaction.repository;

import com.northbank.registration.transaction.domain.model.Transaction;
import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.domain.model.TransactionType;
import com.northbank.registration.transaction.service.dto.TransactionFilter;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> matches(TransactionFilter filter, UUID ownerCustomerId) {
        TransactionFilter safeFilter = filter == null
                ? new TransactionFilter(null, null, null, null, null, null)
                : filter;
        return Specification
                .where(customer(ownerCustomerId != null ? ownerCustomerId : safeFilter.customerId()))
                .and(account(safeFilter.accountId()))
                .and(type(safeFilter.type()))
                .and(status(safeFilter.status()))
                .and(from(safeFilter.from()))
                .and(to(safeFilter.to()));
    }

    private static Specification<Transaction> customer(UUID customerId) {
        return (root, query, cb) -> customerId == null ? cb.conjunction() : cb.equal(root.get("customerId"), customerId);
    }

    private static Specification<Transaction> account(UUID accountId) {
        return (root, query, cb) -> accountId == null
                ? cb.conjunction()
                : cb.or(
                        cb.equal(root.get("sourceAccountId"), accountId),
                        cb.equal(root.get("destinationAccountId"), accountId)
                );
    }

    private static Specification<Transaction> type(TransactionType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    private static Specification<Transaction> status(TransactionStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<Transaction> from(OffsetDateTime from) {
        return (root, query, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("timestamp"), from);
    }

    private static Specification<Transaction> to(OffsetDateTime to) {
        return (root, query, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("timestamp"), to);
    }
}
