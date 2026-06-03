// Story: US-001
package com.northbank.registration.customer.service;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.exception.EmailAlreadyRegisteredException;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.customer.service.dto.RegisterCustomerRequest;
import com.northbank.registration.customer.service.dto.RegisterCustomerResponse;
import com.northbank.registration.customer.service.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business-logic service for customer registration.
 *
 * <p>Responsibilities (per ADR-001):</p>
 * <ol>
 *   <li>Lower-case the email before any operation.</li>
 *   <li>Pre-check email uniqueness → {@link EmailAlreadyRegisteredException} (409).</li>
 *   <li>Hash the password with BCrypt (strength 12) — plaintext is discarded.</li>
 *   <li>Persist the new customer with status {@code PENDING_VERIFICATION} (AC5).</li>
 *   <li>Return only the new {@code id} (AC6).</li>
 * </ol>
 *
 * <p><strong>Security:</strong> the raw password is never logged, never stored,
 * and not included in any return value.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper      customerMapper;
    private final PasswordEncoder     passwordEncoder;

    /**
     * Registers a new customer.
     *
     * @param request validated registration request
     * @return response containing only the new customer's {@code id}
     * @throws EmailAlreadyRegisteredException if the (lower-cased) email is already in use
     */
    public RegisterCustomerResponse registerCustomer(RegisterCustomerRequest request) {

        // 1. Normalise email to lower-case (ADR-001)
        String normalisedEmail = request.email().toLowerCase();

        // 2. Pre-check for duplicate email → friendly 409
        //    The DB UNIQUE constraint is the race-safe source of truth;
        //    DataIntegrityViolationException is handled in GlobalExceptionHandler.
        if (customerRepository.existsByEmail(normalisedEmail)) {
            log.warn("Registration rejected — email already registered: {}", normalisedEmail);
            throw new EmailAlreadyRegisteredException(normalisedEmail);
        }

        // 3. Hash the password — raw value is used once and then GC'd
        //    NEVER log the password or the hash
        String passwordHash = passwordEncoder.encode(request.password());

        // 4. Build and persist the entity (status defaults to PENDING_VERIFICATION)
        Customer customer = customerMapper.toEntity(request, passwordHash);
        customer.setEmail(normalisedEmail);  // ensure mapper output also has lower-cased email

        Customer saved = customerRepository.save(customer);

        log.info("Customer registered successfully — id={}, status={}", saved.getId(), saved.getStatus());

        // 5. Return only the id — AC6
        return customerMapper.toResponse(saved);
    }
}
