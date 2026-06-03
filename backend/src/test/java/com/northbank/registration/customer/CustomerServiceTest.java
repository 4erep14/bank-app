// Story: US-001
package com.northbank.registration.customer;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.exception.EmailAlreadyRegisteredException;
import com.northbank.registration.customer.repository.CustomerRepository;
import com.northbank.registration.customer.service.CustomerService;
import com.northbank.registration.customer.service.dto.RegisterCustomerRequest;
import com.northbank.registration.customer.service.dto.RegisterCustomerResponse;
import com.northbank.registration.customer.service.mapper.CustomerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerService}.
 * Uses Mockito — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService unit tests — US-001")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterCustomerRequest validRequest() {
        return RegisterCustomerRequest.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .email("Ada.Lovelace@Example.COM")     // mixed-case to test normalisation
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 12, 10))
                .password("Str0ng!Pass")
                .build();
    }

    private Customer stubCustomer(UUID id) {
        return Customer.builder()
                .id(id)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada.lovelace@example.com")
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 12, 10))
                .passwordHash("$2a$12$hashed")
                .status(CustomerStatus.PENDING_VERIFICATION)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Happy path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("registerCustomer — happy path: persists with hashed password, returns id")
    void registerCustomer_happyPath_persistsAndReturnsId() {
        UUID customerId = UUID.randomUUID();
        Customer savedCustomer = stubCustomer(customerId);

        when(customerRepository.existsByEmail("ada.lovelace@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("$2a$12$hashed");
        when(customerMapper.toEntity(any(RegisterCustomerRequest.class), eq("$2a$12$hashed")))
                .thenReturn(savedCustomer);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(new RegisterCustomerResponse(customerId));

        RegisterCustomerResponse response = customerService.registerCustomer(validRequest());

        assertThat(response.id()).isEqualTo(customerId);
        verify(customerRepository).save(any(Customer.class));
        verify(passwordEncoder).encode("Str0ng!Pass");
    }

    @Test
    @DisplayName("registerCustomer — email is lower-cased before existence check and persistence")
    void registerCustomer_emailIsNormalisedToLowerCase() {
        UUID customerId = UUID.randomUUID();
        Customer savedCustomer = stubCustomer(customerId);

        when(customerRepository.existsByEmail("ada.lovelace@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(customerMapper.toEntity(any(), anyString())).thenReturn(savedCustomer);
        when(customerRepository.save(any())).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(new RegisterCustomerResponse(customerId));

        customerService.registerCustomer(validRequest());

        // Verify existsByEmail received the lower-cased email
        verify(customerRepository).existsByEmail("ada.lovelace@example.com");

        // Verify the entity saved has the lower-cased email
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ada.lovelace@example.com");
    }

    @Test
    @DisplayName("registerCustomer — status is PENDING_VERIFICATION after save")
    void registerCustomer_statusIsPendingVerification() {
        UUID customerId = UUID.randomUUID();
        Customer savedCustomer = stubCustomer(customerId);

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(customerMapper.toEntity(any(), anyString())).thenReturn(savedCustomer);
        when(customerRepository.save(any())).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(new RegisterCustomerResponse(customerId));

        customerService.registerCustomer(validRequest());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("registerCustomer — password is hashed; raw value not stored")
    void registerCustomer_passwordIsHashed_rawValueNotStored() {
        UUID customerId = UUID.randomUUID();
        Customer savedCustomer = stubCustomer(customerId);

        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("$2a$12$bcryptHash");
        when(customerMapper.toEntity(any(), eq("$2a$12$bcryptHash"))).thenReturn(savedCustomer);
        when(customerRepository.save(any())).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(new RegisterCustomerResponse(customerId));

        customerService.registerCustomer(validRequest());

        // Confirm BCrypt was invoked with the raw password
        verify(passwordEncoder).encode("Str0ng!Pass");

        // Confirm the mapper received the hash, not the raw password
        verify(customerMapper).toEntity(any(RegisterCustomerRequest.class), eq("$2a$12$bcryptHash"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC2 — Duplicate email
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("registerCustomer — duplicate email throws EmailAlreadyRegisteredException")
    void registerCustomer_duplicateEmail_throwsEmailAlreadyRegisteredException() {
        when(customerRepository.existsByEmail("ada.lovelace@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.registerCustomer(validRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verifyNoInteractions(passwordEncoder);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerCustomer — duplicate email check uses lower-cased email")
    void registerCustomer_duplicateEmailCheck_usesLowerCasedEmail() {
        when(customerRepository.existsByEmail("ada.lovelace@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.registerCustomer(validRequest()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(customerRepository).existsByEmail("ada.lovelace@example.com");
        verify(customerRepository, never()).existsByEmail("Ada.Lovelace@Example.COM");
    }
}
