// Story: US-010
package com.northbank.registration.transaction.repository;

import com.northbank.registration.transaction.domain.model.Transaction;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    long countByCustomerIdAndTimestampAfter(UUID customerId, OffsetDateTime timestamp);
}
