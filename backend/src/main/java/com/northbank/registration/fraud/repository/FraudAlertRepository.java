// Story: US-015 / US-017 / US-018
package com.northbank.registration.fraud.repository;

import com.northbank.registration.fraud.domain.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID>, JpaSpecificationExecutor<FraudAlert> {
    Optional<FraudAlert> findByTransactionId(UUID transactionId);
}
