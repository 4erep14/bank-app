// Story: US-002
package com.northbank.registration.auth.login;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security {@link UserDetailsService} implementation backed by the
 * {@code customers} table (ADR-002).
 *
 * <p>This bean is required by the {@code DaoAuthenticationProvider} registered in
 * {@code SecurityConfig}. Credentials are still checked <em>explicitly</em> in
 * {@code AuthService} — this class exists purely to satisfy the Spring Security
 * contract and to prevent Spring Boot from auto-configuring an in-memory
 * {@code UserDetailsService}.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    /**
     * Loads a {@link UserDetails} by normalised (lower-cased) email.
     *
     * @param username the customer's email (used as the Spring Security username)
     * @throws UsernameNotFoundException if no customer exists with the given email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = customerRepository
                .findByEmail(username.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No customer found with email: " + username));

        return User.builder()
                .username(customer.getEmail())
                .password(customer.getPasswordHash())
                .roles("CUSTOMER")
                .accountLocked(customer.getStatus() == CustomerStatus.LOCKED)
                .build();
    }
}
