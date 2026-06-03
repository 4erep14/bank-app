// Story: US-001
package com.northbank.registration.config;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared base class for all integration tests in the registration service.
 *
 * <p><strong>Container lifecycle:</strong> the PostgreSQL container is started
 * <em>once per JVM</em> using the Testcontainers singleton pattern (static
 * initializer block). Ryuk cleans it up at JVM exit. This avoids the
 * start/stop-per-class behaviour of {@code @Testcontainers + @Container static}
 * in a shared base class, which would stop the container after the first
 * concrete subclass finishes and invalidate the cached Spring context used by
 * subsequent subclasses.</p>
 *
 * <p><strong>Spring context caching:</strong> because all concrete subclasses
 * share the same {@code @SpringBootTest} configuration, Spring Test caches a
 * single application context for the entire test suite. The singleton container
 * ensures the datasource URL never changes between test classes.</p>
 *
 * <p><strong>Data isolation:</strong> each test method is wrapped in a
 * transaction that is rolled back after the test, so no manual teardown is
 * required for entity-manager operations. Concrete test classes also add a
 * {@code @BeforeEach deleteAll()} as a safety net for data committed by
 * full HTTP-stack requests.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    // ── Singleton container ───────────────────────────────────────────────────
    // Started ONCE when this class is first loaded; never stopped until JVM exit.
    // Removing @Testcontainers / @Container prevents per-class lifecycle management
    // that would stop the container (and break the shared Spring context cache)
    // after the first concrete subclass completes.
    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
