// Story: US-005
package com.northbank.registration.profile;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProfileService} — US-005.
 *
 * <p>Uses Mockito only — no Spring context, no database.</p>
 *
 * <p>Acceptance Criteria covered:</p>
 * <ul>
 *   <li>AC1 — getProfile returns all five fields correctly</li>
 *   <li>AC2 — updateProfile applies only non-null fields (partial update)</li>
 *   <li>AC5 — changes persisted and visible on next GET</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService unit tests — US-005")
class ProfileServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ProfileService profileService;

    private UUID customerId;
    private Customer existingCustomer;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        existingCustomer = Customer.builder()
                .id(customerId)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada.lovelace@example.com")
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 12, 10))
                .passwordHash("$2a$12$hash")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("AC1 — returns all five profile fields for authenticated customer")
        void ac1_getProfile_returnsAllFields() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));

            ProfileResponse response = profileService.getProfile(customerId);

            assertThat(response.firstName()).isEqualTo("Ada");
            assertThat(response.lastName()).isEqualTo("Lovelace");
            assertThat(response.email()).isEqualTo("ada.lovelace@example.com");
            assertThat(response.phoneNumber()).isEqualTo("+14155552671");
            assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 12, 10));

            verify(customerRepository).findById(customerId);
            verifyNoMoreInteractions(customerRepository);
        }

        @Test
        @DisplayName("getProfile — throws EntityNotFoundException when customer does not exist")
        void getProfile_customerNotFound_throwsEntityNotFoundException() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(customerId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(customerId.toString());
        }
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("AC2 — updates all three mutable fields when all are provided")
        void ac2_updateProfile_allFieldsProvided_updatesAll() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Ada");
            request.setLastName("Byron");
            request.setPhoneNumber("+441234567890");

            ProfileResponse response = profileService.updateProfile(customerId, request);

            assertThat(response.firstName()).isEqualTo("Ada");
            assertThat(response.lastName()).isEqualTo("Byron");
            assertThat(response.phoneNumber()).isEqualTo("+441234567890");
            // read-only fields must be unchanged
            assertThat(response.email()).isEqualTo("ada.lovelace@example.com");
            assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 12, 10));
        }

        @Test
        @DisplayName("AC2 — partial update: only phoneNumber provided, names unchanged")
        void ac2_updateProfile_onlyPhoneProvided_namesUnchanged() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setPhoneNumber("+441234567890");
            // firstName and lastName intentionally null (not provided)

            ProfileResponse response = profileService.updateProfile(customerId, request);

            assertThat(response.firstName()).isEqualTo("Ada");       // unchanged
            assertThat(response.lastName()).isEqualTo("Lovelace");   // unchanged
            assertThat(response.phoneNumber()).isEqualTo("+441234567890");
        }

        @Test
        @DisplayName("AC2 — partial update: only firstName provided, other fields unchanged")
        void ac2_updateProfile_onlyFirstNameProvided_othersUnchanged() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Alexandra");

            ProfileResponse response = profileService.updateProfile(customerId, request);

            assertThat(response.firstName()).isEqualTo("Alexandra");
            assertThat(response.lastName()).isEqualTo("Lovelace");      // unchanged
            assertThat(response.phoneNumber()).isEqualTo("+14155552671"); // unchanged
        }

        @Test
        @DisplayName("AC5 — updated entity is saved via repository")
        void ac5_updateProfile_saveIsCalledWithUpdatedEntity() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Ada");
            request.setLastName("Byron");
            request.setPhoneNumber("+441234567890");

            profileService.updateProfile(customerId, request);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());

            Customer saved = captor.getValue();
            assertThat(saved.getFirstName()).isEqualTo("Ada");
            assertThat(saved.getLastName()).isEqualTo("Byron");
            assertThat(saved.getPhoneNumber()).isEqualTo("+441234567890");
            // read-only fields must not have been touched
            assertThat(saved.getEmail()).isEqualTo("ada.lovelace@example.com");
            assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 12, 10));
        }

        @Test
        @DisplayName("AC5 — empty-body PATCH (all null fields) is a no-op: entity saved unchanged")
        void ac5_updateProfile_emptyRequest_noOpSavesUnchangedEntity() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest();
            // all fields null — should not modify anything

            ProfileResponse response = profileService.updateProfile(customerId, request);

            assertThat(response.firstName()).isEqualTo("Ada");
            assertThat(response.lastName()).isEqualTo("Lovelace");
            assertThat(response.phoneNumber()).isEqualTo("+14155552671");

            verify(customerRepository).save(existingCustomer);
        }

        @Test
        @DisplayName("updateProfile — throws EntityNotFoundException when customer does not exist")
        void updateProfile_customerNotFound_throwsEntityNotFoundException() {
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFirstName("Ada");

            assertThatThrownBy(() -> profileService.updateProfile(customerId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(customerId.toString());

            verify(customerRepository, never()).save(any());
        }
    }
}
