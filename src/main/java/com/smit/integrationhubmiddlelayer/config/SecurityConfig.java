package com.smit.integrationhubmiddlelayer.config;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
 * (an OpenID Connect provider), which issues JWT access tokens for this API. Only intranet employees
 * are allowed in; Bitrix extranet users get 403.
 */
@Configuration
public class SecurityConfig
{
    static final String EMPLOYEE_ROLE = "EMPLOYEE"; //$NON-NLS-1$

    private static final String BITRIX_USER_TYPE_CLAIM = "bitrix_user_type"; //$NON-NLS-1$
    private static final String BITRIX_DEPARTMENTS_CLAIM = "bitrix_departments"; //$NON-NLS-1$

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll() //$NON-NLS-1$ //$NON-NLS-2$
                        .anyRequest().hasRole(EMPLOYEE_ROLE))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(employeeAuthenticationConverter())));
        return http.build();
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

    static JwtAuthenticationConverter employeeAuthenticationConverter()
    {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> isIntranetEmployee(jwt)
                ? List.of(new SimpleGrantedAuthority("ROLE_" + EMPLOYEE_ROLE)) //$NON-NLS-1$
                : List.of());
        return converter;
    }

    /**
     * Bitrix marks internal users as USER_TYPE "employee". Older Bitrix versions do not send USER_TYPE;
     * there, intranet users are the ones assigned to at least one department (extranet users never are).
     */
    static boolean isIntranetEmployee(Jwt jwt)
    {
        String userType = jwt.getClaimAsString(BITRIX_USER_TYPE_CLAIM);
        if (userType != null)
        {
            return "employee".equals(userType); //$NON-NLS-1$
        }
        List<String> departments = jwt.getClaimAsStringList(BITRIX_DEPARTMENTS_CLAIM);
        return departments != null && !departments.isEmpty();
    }
}
