package com.smit.integrationhubmiddlelayer.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Every API call requires a Bitrix24 login: the frontend logs users in through the Bitrix MCP server
 * (an OpenID Connect provider), which issues JWT access tokens for this API. What a user may do is
 * decided per level and right (see PermissionPolicy); every endpoint is mapped below, anything
 * not mapped is denied, so a new endpoint cannot become accessible by accident.
 */
@Configuration
public class SecurityConfig
{
    static final String EMPLOYEE_ROLE = "EMPLOYEE"; //$NON-NLS-1$

    /** Level 3 lives under a Company's URL, so it must be matched before the Level 1 "/companies/**" rules. */
    private static final String[] CONFIGURATIONS_MAPPINGS_PATHS = { "/companies/*/configurations", "/companies/*/configurations/**", //$NON-NLS-1$ //$NON-NLS-2$
            "/companies/*/mappings", "/companies/*/mappings/**" }; //$NON-NLS-1$ //$NON-NLS-2$
    /** Resetting a single configuration entry or mapping row to its inherited value edits the configuration: WRITE, not DELETE. */
    private static final String[] CONFIGURATIONS_MAPPINGS_RESET_PATHS = { "/companies/*/configurations/*/entries/*", //$NON-NLS-1$
            "/companies/*/mappings/*/rows/*" }; //$NON-NLS-1$
    private static final String[] INTERFACES_TEMPLATES_PATHS = { "/interfaces", "/interfaces/**" }; //$NON-NLS-1$ //$NON-NLS-2$
    private static final String[] MANDATORS_COMPANIES_PATHS = { "/mandators", "/mandators/**", "/companies/**" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PermissionPolicy permissionPolicy) throws Exception
    {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll() //$NON-NLS-1$ //$NON-NLS-2$
                        .requestMatchers(HttpMethod.GET, "/me", "/dashboard/**").hasRole(EMPLOYEE_ROLE) //$NON-NLS-1$ //$NON-NLS-2$

                        .requestMatchers(HttpMethod.DELETE, CONFIGURATIONS_MAPPINGS_RESET_PATHS).hasAuthority(authority(AccessLevel.CONFIGURATIONS_MAPPINGS, AccessRight.WRITE))
                        .requestMatchers(HttpMethod.GET, CONFIGURATIONS_MAPPINGS_PATHS).hasAuthority(authority(AccessLevel.CONFIGURATIONS_MAPPINGS, AccessRight.READ))
                        .requestMatchers(HttpMethod.DELETE, CONFIGURATIONS_MAPPINGS_PATHS).hasAuthority(authority(AccessLevel.CONFIGURATIONS_MAPPINGS, AccessRight.DELETE))
                        .requestMatchers(CONFIGURATIONS_MAPPINGS_PATHS).hasAuthority(authority(AccessLevel.CONFIGURATIONS_MAPPINGS, AccessRight.WRITE))

                        .requestMatchers(HttpMethod.GET, INTERFACES_TEMPLATES_PATHS).hasAuthority(authority(AccessLevel.INTERFACES_TEMPLATES, AccessRight.READ))
                        .requestMatchers(HttpMethod.DELETE, INTERFACES_TEMPLATES_PATHS).hasAuthority(authority(AccessLevel.INTERFACES_TEMPLATES, AccessRight.DELETE))
                        .requestMatchers(INTERFACES_TEMPLATES_PATHS).hasAuthority(authority(AccessLevel.INTERFACES_TEMPLATES, AccessRight.WRITE))

                        .requestMatchers(HttpMethod.GET, MANDATORS_COMPANIES_PATHS).hasAuthority(authority(AccessLevel.MANDATORS_COMPANIES, AccessRight.READ))
                        .requestMatchers(HttpMethod.DELETE, MANDATORS_COMPANIES_PATHS).hasAuthority(authority(AccessLevel.MANDATORS_COMPANIES, AccessRight.DELETE))
                        .requestMatchers(MANDATORS_COMPANIES_PATHS).hasAuthority(authority(AccessLevel.MANDATORS_COMPANIES, AccessRight.WRITE))

                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter(permissionPolicy))));
        return http.build();
    }

    private static String authority(AccessLevel level, AccessRight right)
    {
        return level.authority(right);
    }

    /**
     * Verifies tokens offline against the provider's published keys, so there is no network call at
     * startup. Only JWT access tokens (typ "at+jwt", RFC 9068) are accepted: the same provider also
     * signs ID tokens with the same key, and those must never pass as API tokens. The audience and
     * client checks bind tokens to this API and to the Integration Hub frontend; tokens the provider
     * issues for other resources (e.g. the MCP server itself) are rejected.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${integration-hub.auth.issuer-uri}") String issuerUri,
            @Value("${integration-hub.auth.audience}") String audience,
            @Value("${integration-hub.auth.client-id:integration-hub}") String clientId) //$NON-NLS-1$
    {
        // An empty AUTH_AUDIENCE would otherwise start fine and reject every login with 401.
        Assert.hasText(audience, "integration-hub.auth.audience (AUTH_AUDIENCE) must be set to this API's public URL"); //$NON-NLS-1$
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuerUri + "/jwks") //$NON-NLS-1$
                .jwtProcessorCustomizer(processor -> processor.setJWSTypeVerifier(
                        new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("at+jwt")))) //$NON-NLS-1$
                .build();

        decoder.setJwtValidator(tokenValidator(issuerUri, audience, clientId));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> tokenValidator(String issuerUri, String audience, String clientId)
    {
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Collection<String>>("aud", //$NON-NLS-1$
                aud -> aud != null && aud.contains(audience));
        // JwtTimestampValidator accepts tokens without exp; an access token must always expire.
        OAuth2TokenValidator<Jwt> expiryRequired = new JwtClaimValidator<Instant>("exp", Objects::nonNull); //$NON-NLS-1$
        OAuth2TokenValidator<Jwt> clientValidator = new JwtClaimValidator<String>("client_id", clientId::equals); //$NON-NLS-1$
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), new JwtIssuerValidator(issuerUri),
                audienceValidator, expiryRequired, clientValidator);
    }

    static JwtAuthenticationConverter authenticationConverter(PermissionPolicy permissionPolicy)
    {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(permissionPolicy::authorities);
        return converter;
    }
}
