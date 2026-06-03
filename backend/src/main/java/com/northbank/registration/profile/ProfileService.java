// Story: US-005
package com.northbank.registration.profile;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for viewing and partially updating the authenticated customer's profile (US-005).
 *
 * <p>Both operations work directly against the existing {@code customers} table —
 * no new migration is required (ADR-005: No New DB Migration).</p>
 *
 * <p>The filter layer ({@code JwtAuthenticationFilter}) guarantees that the
 * {@code customerId} passed to these methods corresponds to an existing customer
 * with a valid, non-invalidated ACCESS token. The repository load here is a
 * within-transaction re-fetch for consistency.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

    private final CustomerRepository customerRepository;

    // ── GET /api/v1/profile ─────────────────────────────────────────────────

    /**
     * Returns the authenticated customer's profile (AC1).
     *
     * @param customerId UUID extracted from the ACCESS JWT {@code sub} claim
     * @return profile DTO with firstName, lastName, email, phoneNumber, dateOfBirth
     * @throws EntityNotFoundException if no customer exists with the given id
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID customerId) {
        Customer customer = findOrThrow(customerId);
        log.debug("Profile fetched for customerId={}", customerId);
        return toResponse(customer);
    }

    // ── PATCH /api/v1/profile ───────────────────────────────────────────────

    /**
     * Partially updates the mutable profile fields (AC2, AC5).
     *
     * <p>Only non-null fields in {@code request} are applied — sending a field
     * as {@code null} (or omitting it entirely from the JSON body) leaves that
     * field unchanged. {@code email} and {@code dateOfBirth} can never be updated
     * via this endpoint; they are blocked at the DTO layer (AC3).</p>
     *
     * @param customerId UUID extracted from the ACCESS JWT {@code sub} claim
     * @param request    partial update payload
     * @return the full updated profile DTO
     * @throws EntityNotFoundException if no customer exists with the given id
     */
    public ProfileResponse updateProfile(UUID customerId, UpdateProfileRequest request) {
        Customer customer = findOrThrow(customerId);

        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        Customer saved = customerRepository.save(customer);
        log.debug("Profile updated for customerId={}", customerId);
        return toResponse(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Customer findOrThrow(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found: " + customerId));
    }

    private ProfileResponse toResponse(Customer c) {
        return new ProfileResponse(
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhoneNumber(),
                c.getDateOfBirth()
        );
    }
}
