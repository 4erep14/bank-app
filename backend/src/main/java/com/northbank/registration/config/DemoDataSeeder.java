// Story: EPIC-005
package com.northbank.registration.config;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.domain.model.BankAccount;
import com.northbank.registration.account.repository.AccountRepository;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerRole;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "northbank.demo", name = "seed-enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String CUSTOMER_PASSWORD = "CustomerPass123!";
    private static final String ADMIN_PASSWORD = "AdminPass123!";

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Customer admin = ensureCustomer(new DemoCustomer(
            "Alex",
            "Morgan",
            "admin@northbank.test",
            "+15550001000",
            LocalDate.of(1985, 4, 17),
            ADMIN_PASSWORD,
            CustomerStatus.ACTIVE,
            CustomerRole.ADMIN,
            0,
            null
        ));

        List<DemoCustomer> customers = demoCustomers();
        for (DemoCustomer demoCustomer : customers) {
            Customer customer = ensureCustomer(demoCustomer);
            seedAccounts(customer);
        }

        log.info("Demo users ready: admin={} customers={}",
            admin.getEmail(),
            customers.size()
        );
    }

    private List<DemoCustomer> demoCustomers() {
        return List.of(
            new DemoCustomer(
                "Maria",
                "Garcia",
                "maria.garcia@northbank.test",
                "+15550001001",
                LocalDate.of(1991, 8, 9),
                CUSTOMER_PASSWORD,
                CustomerStatus.ACTIVE,
                CustomerRole.CUSTOMER,
                0,
                null
            ),
            new DemoCustomer(
                "David",
                "Chen",
                "david.chen@northbank.test",
                "+15550001002",
                LocalDate.of(1988, 1, 24),
                CUSTOMER_PASSWORD,
                CustomerStatus.ACTIVE,
                CustomerRole.CUSTOMER,
                0,
                null
            ),
            new DemoCustomer(
                "Sarah",
                "Patel",
                "sarah.patel@northbank.test",
                "+15550001003",
                LocalDate.of(1994, 11, 3),
                CUSTOMER_PASSWORD,
                CustomerStatus.LOCKED,
                CustomerRole.CUSTOMER,
                5,
                OffsetDateTime.now().minusDays(1)
            ),
            new DemoCustomer(
                "Leo",
                "Martin",
                "leo.martin@northbank.test",
                "+15550001004",
                LocalDate.of(1979, 6, 30),
                CUSTOMER_PASSWORD,
                CustomerStatus.INACTIVE,
                CustomerRole.CUSTOMER,
                0,
                null
            )
        );
    }

    private Customer ensureCustomer(DemoCustomer demo) {
        String email = demo.email().toLowerCase();
        return customerRepository.findByEmail(email)
            .orElseGet(() -> customerRepository.save(Customer.builder()
                .firstName(demo.firstName())
                .lastName(demo.lastName())
                .email(email)
                .phoneNumber(demo.phoneNumber())
                .dateOfBirth(demo.dateOfBirth())
                .passwordHash(passwordEncoder.encode(demo.password()))
                .status(demo.status())
                .role(demo.role())
                .failedLoginAttempts(demo.failedLoginAttempts())
                .lockedAt(demo.lockedAt())
                .passwordChangedAt(OffsetDateTime.now())
                .build()));
    }

    private void seedAccounts(Customer customer) {
        switch (customer.getEmail()) {
            case "maria.garcia@northbank.test" -> {
                ensureAccount(customer, AccountType.CHECKING, "1000001001", "4825.75", AccountStatus.ACTIVE);
                ensureAccount(customer, AccountType.SAVINGS, "1000001002", "18420.00", AccountStatus.ACTIVE);
            }
            case "david.chen@northbank.test" -> {
                ensureAccount(customer, AccountType.CHECKING, "1000001003", "2380.40", AccountStatus.ACTIVE);
                ensureAccount(customer, AccountType.SAVINGS, "1000001004", "8200.00", AccountStatus.ACTIVE);
            }
            case "sarah.patel@northbank.test" -> {
                ensureAccount(customer, AccountType.CHECKING, "1000001005", "380.25", AccountStatus.FROZEN);
                ensureAccount(customer, AccountType.SAVINGS, "1000001006", "1500.00", AccountStatus.ACTIVE);
            }
            case "leo.martin@northbank.test" ->
                ensureAccount(customer, AccountType.CHECKING, "1000001007", "94.12", AccountStatus.INACTIVE);
            default -> {
            }
        }
    }

    private void ensureAccount(
        Customer customer,
        AccountType type,
        String accountNumber,
        String balance,
        AccountStatus status
    ) {
        if (accountRepository.existsByCustomerIdAndType(customer.getId(), type)) {
            return;
        }

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            log.warn("Skipping demo account {} for {} because the account number already exists",
                accountNumber,
                customer.getEmail()
            );
            return;
        }

        accountRepository.save(BankAccount.builder()
            .customerId(customer.getId())
            .accountNumber(accountNumber)
            .type(type)
            .balance(new BigDecimal(balance))
            .status(status)
            .build());
    }

    private record DemoCustomer(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String password,
        CustomerStatus status,
        CustomerRole role,
        int failedLoginAttempts,
        OffsetDateTime lockedAt
    ) {
    }
}
