package com.smit.integrationhubmiddlelayer;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;

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

    private static final String TEST_TOKEN = "integration-test-token"; //$NON-NLS-1$

    @Autowired
    protected TestRestTemplate restTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /**
     * The API requires a Bitrix login (see SecurityConfig). Tests run the real security filter chain
     * and authenticate as an intranet employee: every request carries a test token that the mocked
     * decoder turns into a valid employee JWT.
     */
    @BeforeEach
    void authenticateAsIntranetEmployee()
    {
        Instant now = Instant.now();
        when(jwtDecoder.decode(TEST_TOKEN)).thenReturn(Jwt.withTokenValue(TEST_TOKEN)
                .header("alg", "RS256") //$NON-NLS-1$ //$NON-NLS-2$
                .subject("1") //$NON-NLS-1$
                .claim("bitrix_user_type", "employee") //$NON-NLS-1$ //$NON-NLS-2$
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build());
        ClientHttpRequestInterceptor bearer = (request, body, execution) ->
        {
            request.getHeaders().setBearerAuth(TEST_TOKEN);
            return execution.execute(request, body);
        };
        restTemplate.getRestTemplate().setInterceptors(List.of(bearer));
    }

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
