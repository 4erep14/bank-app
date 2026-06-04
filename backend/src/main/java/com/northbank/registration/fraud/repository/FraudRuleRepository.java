// Story: US-014
package com.northbank.registration.fraud.repository;

import com.northbank.registration.fraud.domain.model.FraudRule;
import com.northbank.registration.fraud.domain.model.FraudRuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    long countByActiveTrueAndStatus(FraudRuleStatus status);
    List<FraudRule> findAllByActiveTrueAndStatus(FraudRuleStatus status);
    Page<FraudRule> findAllByStatusNot(FraudRuleStatus status, Pageable pageable);
}
