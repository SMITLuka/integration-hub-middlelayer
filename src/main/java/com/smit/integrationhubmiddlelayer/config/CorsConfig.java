package com.smit.integrationhubmiddlelayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures CORS to allow browser-based UI clients to call this middlelayer.
 * Allowed origins are controlled via the integration-hub.cors.allowed-origins configuration property.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer
{
    @Value("${integration-hub.cors.allowed-origins:*}") //$NON-NLS-1$
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry)
    {
        registry.addMapping("/**") //$NON-NLS-1$
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                .allowedHeaders("Content-Type") //$NON-NLS-1$
                .allowCredentials(false)
                .maxAge(3600L);
    }
}
