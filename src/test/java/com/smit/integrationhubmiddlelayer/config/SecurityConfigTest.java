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
import java.util.List;
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
 * endpoint: a Bitrix login is required, and only intranet employees are let in.
 */
@WebMvcTest(MandatorController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class, PermissionPolicy.class})
class SecurityConfigTest
{
    private static final String ISSUER = "https://mcp.sm-it.hr"; //$NON-NLS-1$
    private static final String AUDIENCE = "https://integration-hub.example.test"; //$NON-NLS-1$

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
    void request_returns200_forIntranetEmployee() throws Exception
    {
        givenToken(Map.of("bitrix_user_type", "employee", "bitrix_departments", List.of(1))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        when(mandatorService.list(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/mandators").header("Authorization", "Bearer user-token")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isOk());
    }

    @Test
    void request_returns403_forExtranetUser() throws Exception
    {
        givenToken(Map.of("bitrix_user_type", "extranet", "bitrix_departments", List.of())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        mockMvc.perform(get("/mandators").header("Authorization", "Bearer user-token")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isForbidden());
    }

    @Test
    void request_usesDepartments_whenBitrixSendsNoUserType() throws Exception
    {
        givenToken(Map.of("bitrix_departments", List.of(5))); //$NON-NLS-1$
        when(mandatorService.list(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/mandators").header("Authorization", "Bearer user-token")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isOk());

        givenToken(Map.of("bitrix_departments", List.of())); //$NON-NLS-1$
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
    void tokenValidator_acceptsOwnAudience_rejectsOtherAudienceAndIssuer()
    {
        OAuth2TokenValidator<Jwt> validator = SecurityConfig.tokenValidator(ISSUER, AUDIENCE, "integration-hub"); //$NON-NLS-1$

        assertThat(validator.validate(jwt(ISSUER, AUDIENCE, Map.of())).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(ISSUER, "https://mcp.sm-it.hr/mcp", Map.of())).hasErrors()).isTrue(); //$NON-NLS-1$
        assertThat(validator.validate(jwt("https://evil.example.test", AUDIENCE, Map.of())).hasErrors()).isTrue(); //$NON-NLS-1$
    }

    @Test
    void tokenValidator_rejectsTokenIssuedToAnotherClient()
    {
        OAuth2TokenValidator<Jwt> validator = SecurityConfig.tokenValidator(ISSUER, AUDIENCE, "integration-hub"); //$NON-NLS-1$

        assertThat(validator.validate(jwt(ISSUER, AUDIENCE, Map.of("client_id", "some-dcr-client"))).hasErrors()).isTrue(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void jwtDecoder_refusesToStart_whenAudienceIsBlank()
    {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new SecurityConfig().jwtDecoder(ISSUER, " ", "integration-hub")) //$NON-NLS-1$ //$NON-NLS-2$
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AUTH_AUDIENCE"); //$NON-NLS-1$
    }

    private void givenToken(Map<String, Object> claims)
    {
        when(jwtDecoder.decode("user-token")).thenReturn(jwt(ISSUER, AUDIENCE, claims)); //$NON-NLS-1$
    }

    private static Jwt jwt(String issuer, String audience, Map<String, Object> claims)
    {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token") //$NON-NLS-1$
                .header("alg", "RS256") //$NON-NLS-1$ //$NON-NLS-2$
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("1") //$NON-NLS-1$
                .claim("client_id", "integration-hub") //$NON-NLS-1$ //$NON-NLS-2$
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claims(existing -> existing.putAll(claims))
                .build();
    }
}
