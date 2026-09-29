package com.smit.integrationhubmiddlelayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base for full-stack integration tests: boots the application on a random port
 * against a single, test-run-wide real Postgres container (Testcontainers), with Flyway
 * migrating the actual schema on startup. Subclasses get a real {@link TestRestTemplate}
 * wired to that running instance, so they exercise the full controller -> service ->
 * repository -> database stack rather than mocking any layer.
 * <p>
 * The container is started once per JVM (static field, no explicit stop) and reused across
 * every test class that extends this base, which is the standard Testcontainers singleton
 * pattern and avoids paying container startup cost per test class.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public abstract class AbstractIntegrationTest
{
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine") //$NON-NLS-1$
            .withDatabaseName("integration_hub") //$NON-NLS-1$
            .withUsername("integration_hub") //$NON-NLS-1$
            .withPassword("integration_hub"); //$NON-NLS-1$

    static
    {
        POSTGRES.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Points the application's datasource at the shared Testcontainers Postgres instance
     * instead of the {@code localhost:5432} default from application.yml.
     *
     * @param registry the dynamic property registry supplied by Spring's test context
     */
    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl); //$NON-NLS-1$
        registry.add("spring.datasource.username", POSTGRES::getUsername); //$NON-NLS-1$
        registry.add("spring.datasource.password", POSTGRES::getPassword); //$NON-NLS-1$
    }
}
