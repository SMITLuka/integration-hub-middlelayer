package com.smit.integrationhubmiddlelayer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * Separate security chain for the machine-to-machine API under /service/**.
 * Callers (other SMIT services such as MIMW) authenticate with a shared API key sent in the
 * X-Api-Key header; valid keys come from SERVICE_API_KEYS (comma separated, so a key can be
 * rotated without downtime). With no keys configured every /service request is rejected.
 * Only GET is allowed: the service API is read-only.
 */
@Configuration
public class ServiceApiSecurityConfig
{
    static final String API_KEY_HEADER = "X-Api-Key"; //$NON-NLS-1$
    static final String SERVICE_ROLE = "SERVICE"; //$NON-NLS-1$

    private static final Logger log = LoggerFactory.getLogger(ServiceApiSecurityConfig.class);

    @Bean
    @Order(1)
    public SecurityFilterChain serviceApiSecurityFilterChain(HttpSecurity http,
            @Value("${integration-hub.service-api.keys:}") String keys) throws Exception //$NON-NLS-1$
    {
        List<byte[]> validKeys = Arrays.stream(keys.split(",")) //$NON-NLS-1$
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .map(k -> k.getBytes(StandardCharsets.UTF_8))
                .toList();
        if (validKeys.isEmpty())
        {
            log.warn("integration-hub.service-api.keys (SERVICE_API_KEYS) is empty: every /service request will be rejected"); //$NON-NLS-1$
        }

        http
                .securityMatcher("/service/**") //$NON-NLS-1$
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ApiKeyFilter(validKeys), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/service/**").hasRole(SERVICE_ROLE) //$NON-NLS-1$
                        .anyRequest().denyAll());
        return http.build();
    }

    static final class ApiKeyFilter extends OncePerRequestFilter
    {
        private final List<byte[]> validKeys;

        ApiKeyFilter(List<byte[]> validKeys)
        {
            this.validKeys = validKeys;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException
        {
            String presented = request.getHeader(API_KEY_HEADER);
            if (presented == null || !isValid(presented.getBytes(StandardCharsets.UTF_8)))
            {
                log.warn("Rejected /service request from {}: {} API key", request.getRemoteAddr(), //$NON-NLS-1$
                        presented == null ? "missing" : "invalid"); //$NON-NLS-1$ //$NON-NLS-2$
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid X-Api-Key\"}"); //$NON-NLS-1$
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("service", null, //$NON-NLS-1$
                    List.of(new SimpleGrantedAuthority("ROLE_" + SERVICE_ROLE))); //$NON-NLS-1$
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }

        /** Checks every configured key with a constant-time comparison so timing does not leak which bytes matched. */
        private boolean isValid(byte[] presented)
        {
            boolean match = false;
            for (byte[] key : validKeys)
            {
                match |= MessageDigest.isEqual(key, presented);
            }
            return match;
        }
    }
}
