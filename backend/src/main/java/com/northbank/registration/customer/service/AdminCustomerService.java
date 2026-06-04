// Story: US-019
package com.northbank.registration.customer.service;

import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.service.AuditLogService;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerRole;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.exception.AdminCustomerDeactivationException;
import com.northbank.registration.customer.exception.CustomerNotFoundException;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.customer.service.dto.AdminCustomerSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCustomerService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminCustomerSummaryResponse> listCustomers(CustomerStatus status, int page, int size) {
        int resolvedSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        Specification<Customer> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return customerRepository.findAll(
                        spec,
                        PageRequest.of(page, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(this::toSummary);
    }

    @Transactional
    public AdminCustomerSummaryResponse deactivateCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
        if (customer.getRole() == CustomerRole.ADMIN) {
            throw new AdminCustomerDeactivationException();
        }

        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setPasswordChangedAt(OffsetDateTime.now());
        customer.setFailedLoginAttempts(0);
        customer.setLockedAt(null);

        Customer saved = customerRepository.save(customer);
        auditLogService.record(AuditActionType.CUSTOMER_DEACTIVATED, "CUSTOMER", saved.getId());
        log.info("Admin deactivated customer id={}", saved.getId());
        return toSummary(saved);
    }

    @Transactional
    public AdminCustomerSummaryResponse unlockCustomer(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setFailedLoginAttempts(0);
        customer.setLockedAt(null);

        Customer saved = customerRepository.save(customer);
        auditLogService.record(AuditActionType.CUSTOMER_UNLOCKED, "CUSTOMER", saved.getId());
        log.info("Admin unlocked customer id={}", saved.getId());
        return toSummary(saved);
    }

    private AdminCustomerSummaryResponse toSummary(Customer customer) {
        return new AdminCustomerSummaryResponse(
                customer.getId(),
                (customer.getFirstName() + " " + customer.getLastName()).trim(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getStatus(),
                customer.getCreatedAt()
        );
    }
}
