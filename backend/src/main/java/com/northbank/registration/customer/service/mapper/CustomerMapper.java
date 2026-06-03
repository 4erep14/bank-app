// Story: US-001 / US-002
package com.northbank.registration.customer.service.mapper;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.service.dto.RegisterCustomerRequest;
import com.northbank.registration.customer.service.dto.RegisterCustomerResponse;
import org.mapstruct.*;

/**
 * MapStruct mapper — converts between the registration DTO and the
 * {@link Customer} JPA entity, and from entity to response DTO.
 *
 * <p>{@code password} is explicitly ignored during entity mapping: the
 * service hashes it separately before calling {@link #toEntity}.</p>
 *
 * <p>US-002 fields ({@code failedLoginAttempts}, {@code lockedAt},
 * {@code passwordChangedAt}) are ignored here — they are managed by
 * {@code AuthService}.</p>
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerMapper {

    /**
     * Maps a registration request + pre-computed BCrypt hash into a new
     * {@link Customer} entity ready to be persisted.
     *
     * @param request      validated registration request (password field ignored here)
     * @param passwordHash BCrypt(strength=12) hash produced by the service
     * @return transient Customer entity (id and timestamps set by JPA/DB)
     */
    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "createdAt",            ignore = true)
    @Mapping(target = "updatedAt",            ignore = true)
    @Mapping(target = "status",               ignore = true)   // set by @Builder.Default = PENDING_VERIFICATION
    @Mapping(target = "failedLoginAttempts",  ignore = true)   // US-002: managed by AuthService
    @Mapping(target = "lockedAt",             ignore = true)   // US-002: managed by AuthService
    @Mapping(target = "passwordChangedAt",    ignore = true)   // US-002/004: managed by PasswordResetService
    @Mapping(target = "passwordHash",         source = "passwordHash")
    @Mapping(target = "email",                source = "request.email")
    Customer toEntity(RegisterCustomerRequest request, String passwordHash);

    /**
     * Maps a persisted {@link Customer} entity to the slim 201 response DTO.
     * Only the {@code id} field is included — AC6.
     */
    RegisterCustomerResponse toResponse(Customer customer);
}
