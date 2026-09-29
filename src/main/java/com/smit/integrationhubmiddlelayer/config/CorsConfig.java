package com.smit.integrationhubmiddlelayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures CORS to allow browser-based UI clients to call this middlelayer.
 * Allowed origins are controlled via the integration-hub.cors.allowed-origins configuration property.
 * Exposed as a CorsConfigurationSource because Spring Security (SecurityConfig) must answer CORS
 * pre-flight requests itself: they carry no token and would otherwise be rejected with 401 before
 * reaching Spring MVC.
 */
@Configuration
public class CorsConfig
{
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${integration-hub.cors.allowed-origins:*}") String allowedOrigins) //$NON-NLS-1$
    {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization")); //$NON-NLS-1$ //$NON-NLS-2$
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); //$NON-NLS-1$
        return source;
    }
}
