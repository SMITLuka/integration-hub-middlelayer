package com.smit.integrationhubmiddlelayer.config;

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
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;

/**
 * Every API call requires an SMIT SSO login: users open Integration Hub from the SSO portal, which
 * hands the frontend a signed JWT issued for this app. What a user may do is decided per level and
 * right from the SSO permissions in that token (see PermissionPolicy); every endpoint is mapped
 * below, anything not mapped is denied, so a new endpoint cannot become accessible by accident.
 */
@Configuration
public class SecurityConfig
{
    static final String EMPLOYEE_ROLE = "EMPLOYEE"; //$NON-NLS-1$
    static final String APP_ID_CLAIM = "app_id"; //$NON-NLS-1$

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
     * Verifies tokens offline against the SSO gateway's published keys, so there is no network call at
     * startup. SSO tokens carry neither "iss" nor "aud"; the binding to this API is the "app_id" claim,
     * which the gateway sets to the registered app the token was issued for. Tokens for other SSO apps
     * and portal-only tokens (no app_id) are rejected, otherwise any SSO app's token would open this API.
     * Revocation is deliberately not checked (the gateway's /verify is rate-limited to 60 calls a minute):
     * a deactivated SSO user or a removed role keeps working until the token expires, at most one hour.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${integration-hub.auth.jwks-uri}") String jwksUri, //$NON-NLS-1$
            @Value("${integration-hub.auth.app-id}") String appId) //$NON-NLS-1$
    {
        // An empty SSO_APP_ID would otherwise start fine and reject every login with 401.
        Assert.hasText(jwksUri, "integration-hub.auth.jwks-uri (SSO_JWKS_URI) must be set"); //$NON-NLS-1$
        Assert.hasText(appId, "integration-hub.auth.app-id (SSO_APP_ID) must be set to this app's ID in the SSO gateway"); //$NON-NLS-1$
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(tokenValidator(appId));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> tokenValidator(String appId)
    {
        // A missing claim reaches the predicate as null, so tokens without app_id fail too; Object, so a
        // non-string app_id is a validation error (401) rather than a ClassCastException (500).
        OAuth2TokenValidator<Jwt> appValidator = new JwtClaimValidator<Object>(APP_ID_CLAIM, appId::equals);
        // JwtTimestampValidator accepts tokens without exp; an access token must always expire.
        OAuth2TokenValidator<Jwt> expiryRequired = new JwtClaimValidator<Instant>("exp", Objects::nonNull); //$NON-NLS-1$
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), expiryRequired, appValidator);
    }

    static JwtAuthenticationConverter authenticationConverter(PermissionPolicy permissionPolicy)
    {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(permissionPolicy::authorities);
        return converter;
    }
}
