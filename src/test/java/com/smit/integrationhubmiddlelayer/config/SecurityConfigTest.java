package com.smit.integrationhubmiddlelayer.config;

import com.smit.integrationhubmiddlelayer.controller.MandatorController;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import com.smit.integrationhubmiddlelayer.service.MandatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real security filter chain (with a mocked token decoder) against one representative
 * endpoint: an SSO login is required, and only users with an Integration Hub flag in SSO are let in.
 */
@WebMvcTest(MandatorController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class, PermissionPolicy.class})
class SecurityConfigTest
{
    private static final String APP_ID = "e9e995e9-4046-43d0-ac03-ee3c563c9f7f"; //$NON-NLS-1$
    private static final String JWKS_URI = "https://sso.example.test/.well-known/jwks.json"; //$NON-NLS-1$

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MandatorService mandatorService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void request_returns401_withoutToken() throws Exception
    {
        mockMvc.perform(get("/mandators")) //$NON-NLS-1$
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_returns401_whenTokenInvalid() throws Exception
    {
        when(jwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("invalid signature")); //$NON-NLS-1$ //$NON-NLS-2$

        mockMvc.perform(get("/mandators").header("Authorization", "Bearer bad-token")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_returns200_forUserWithIntegrationHubAccess() throws Exception
    {
        givenToken(Map.of("is_app_user", true)); //$NON-NLS-1$
        when(mandatorService.list(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/mandators").header("Authorization", "Bearer user-token")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isOk());
    }

    @Test
    void request_returns403_forSsoUserWithoutIntegrationHubFlag() throws Exception
    {
        givenToken(Map.of());

        mockMvc.perform(get("/mandators").header("Authorization", "Bearer user-token")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isForbidden());
    }

    @Test
    void corsPreflight_isAnsweredWithoutToken_andAllowsAuthorizationHeader() throws Exception
    {
        mockMvc.perform(options("/mandators") //$NON-NLS-1$
                        .header("Origin", "https://integration-hub-middlelayer-fronten.vercel.app") //$NON-NLS-1$ //$NON-NLS-2$
                        .header("Access-Control-Request-Method", "POST") //$NON-NLS-1$ //$NON-NLS-2$
                        .header("Access-Control-Request-Headers", "authorization,content-type")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsStringIgnoringCase("authorization"))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void tokenValidator_acceptsTokenForThisApp_rejectsOtherAppAndPortalTokens()
    {
        OAuth2TokenValidator<Jwt> validator = SecurityConfig.tokenValidator(APP_ID);

        assertThat(validator.validate(jwt(APP_ID, Map.of())).hasErrors()).isFalse();
        // e.g. a token the gateway issued for SMIT Pulse
        assertThat(validator.validate(jwt("d74c5004-8a63-48c9-b54b-3be0b3154b3f", Map.of())).hasErrors()).isTrue(); //$NON-NLS-1$
        // portal-only tokens carry no app_id at all
        assertThat(validator.validate(jwt(null, Map.of())).hasErrors()).isTrue();
    }

    @Test
    void jwtDecoder_refusesToStart_whenAppIdIsBlank()
    {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new SecurityConfig().jwtDecoder(JWKS_URI, " ")) //$NON-NLS-1$
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SSO_APP_ID"); //$NON-NLS-1$
    }

    private void givenToken(Map<String, Object> featureFlags)
    {
        when(jwtDecoder.decode("user-token")).thenReturn(jwt(APP_ID, featureFlags)); //$NON-NLS-1$
    }

    private static Jwt jwt(String appId, Map<String, Object> featureFlags)
    {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("token") //$NON-NLS-1$
                .header("alg", "RS256") //$NON-NLS-1$ //$NON-NLS-2$
                .subject("1") //$NON-NLS-1$
                .claim("permissions", Map.of("feature_flags", featureFlags, "data_scope", Map.of())) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600));
        if (appId != null)
        {
            builder.claim("app_id", appId); //$NON-NLS-1$
        }
        return builder.build();
    }
}
