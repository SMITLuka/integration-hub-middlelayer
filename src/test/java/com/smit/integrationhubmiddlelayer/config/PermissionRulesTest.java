package com.smit.integrationhubmiddlelayer.config;

import com.smit.integrationhubmiddlelayer.controller.CurrentUserController;
import com.smit.integrationhubmiddlelayer.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The full permission matrix per endpoint, for an ordinary intranet employee and for a member of an
 * admin workgroup. Only the security rules are under test: the web slice contains just /me, so a
 * request that passes security ends as 404/405, while a request that is blocked ends as 403.
 */
@WebMvcTest(CurrentUserController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class, CorsConfig.class, PermissionPolicy.class })
@TestPropertySource(properties = "integration-hub.auth.admin-workgroups=42") //$NON-NLS-1$
class PermissionRulesTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void tokens()
    {
        when(jwtDecoder.decode("employee")).thenReturn(jwt(Map.of("bitrix_user_type", "employee", "bitrix_workgroups", List.of("7")))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        when(jwtDecoder.decode("admin")).thenReturn(jwt(Map.of("bitrix_user_type", "employee", "bitrix_workgroups", List.of("7", "42")))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        when(jwtDecoder.decode("extranet")).thenReturn(jwt(Map.of("bitrix_user_type", "extranet", "bitrix_workgroups", List.of("45")))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    @ParameterizedTest(name = "{0} {1}: employee allowed={2}, admin allowed={3}")
    @CsvSource({
            // Level 1: Mandators & Companies - employees read, admins everything
            "GET,    /mandators,                                 true,  true",
            "GET,    /mandators/1,                               true,  true",
            "POST,   /mandators,                                 false, true",
            "PUT,    /mandators/1,                               false, true",
            "PUT,    /mandators/1/additional-data/KEY,           false, true",
            "DELETE, /mandators/1,                               false, true",
            "POST,   /mandators/1/companies,                     false, true",
            "GET,    /companies/1,                               true,  true",
            "PUT,    /companies/1,                               false, true",
            "DELETE, /companies/1/additional-data/KEY,           false, true",
            "DELETE, /companies/1,                               false, true",
            // Level 2: Interfaces & Templates - employees read, admins everything
            "GET,    /interfaces,                                true,  true",
            "GET,    /interfaces/1/mapping-template,             true,  true",
            "POST,   /interfaces,                                false, true",
            "PUT,    /interfaces/1,                              false, true",
            "PUT,    /interfaces/1/configuration-template,       false, true",
            "DELETE, /interfaces/1/mapping-template,             false, true",
            "DELETE, /interfaces/1,                              false, true",
            // Level 3: Configurations & Mappings - employees read and write (incl. resetting a value), admins also delete
            "GET,    /companies/1/configurations,                true,  true",
            "GET,    /companies/1/configurations/2,              true,  true",
            "POST,   /companies/1/configurations,                true,  true",
            "PUT,    /companies/1/configurations/2/entries/3,    true,  true",
            "DELETE, /companies/1/configurations/2/entries/3,    true,  true",
            "DELETE, /companies/1/configurations/2,              false, true",
            "GET,    /companies/1/mappings/2,                    true,  true",
            "POST,   /companies/1/mappings,                      true,  true",
            "PUT,    /companies/1/mappings/2/rows/3,             true,  true",
            "DELETE, /companies/1/mappings/2/rows/3,             true,  true",
            "DELETE, /companies/1/mappings/2,                    false, true",
            // Everyone
            "GET,    /dashboard/summary,                         true,  true",
            "GET,    /me,                                        true,  true",
            // Not mapped: denied for everyone
            "GET,    /something-new,                             false, false",
            "POST,   /dashboard/summary,                         false, false",
    })
    void permissionMatrix(String method, String path, boolean employeeAllowed, boolean adminAllowed) throws Exception
    {
        assertThat(isAllowed("employee", method, path)).as("employee").isEqualTo(employeeAllowed); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(isAllowed("admin", method, path)).as("admin").isEqualTo(adminAllowed); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(isAllowed("extranet", method, path)).as("extranet").isFalse(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void me_returnsNameEmailAndTheEmployeesPermissions() throws Exception
    {
        mockMvc.perform(get("/me").header("Authorization", "Bearer employee")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luka Lozic")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.email").value("luka@example.test")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.permissions[0].level").value("MANDATORS_COMPANIES")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.permissions[0].read").value(true)) //$NON-NLS-1$
                .andExpect(jsonPath("$.permissions[0].write").value(false)) //$NON-NLS-1$
                .andExpect(jsonPath("$.permissions[2].level").value("CONFIGURATIONS_MAPPINGS")) //$NON-NLS-1$ //$NON-NLS-2$
                .andExpect(jsonPath("$.permissions[2].write").value(true)) //$NON-NLS-1$
                .andExpect(jsonPath("$.permissions[2].delete").value(false)); //$NON-NLS-1$
    }

    @Test
    void me_showsAllRightsForAdminWorkgroupMember() throws Exception
    {
        mockMvc.perform(get("/me").header("Authorization", "Bearer admin")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[*].delete").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true)))); //$NON-NLS-1$
    }

    private boolean isAllowed(String token, String method, String path) throws Exception
    {
        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(method), path)
                .header("Authorization", "Bearer " + token) //$NON-NLS-1$ //$NON-NLS-2$
                .contentType("application/json").content("{}")) //$NON-NLS-1$ //$NON-NLS-2$
                .andReturn();
        int statusCode = result.getResponse().getStatus();
        assertThat(statusCode).as("token must be accepted").isNotEqualTo(401); //$NON-NLS-1$
        return statusCode != 403;
    }

    private static Jwt jwt(Map<String, Object> claims)
    {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token") //$NON-NLS-1$
                .header("alg", "RS256") //$NON-NLS-1$ //$NON-NLS-2$
                .subject("7") //$NON-NLS-1$
                .claim("name", "Luka Lozic") //$NON-NLS-1$ //$NON-NLS-2$
                .claim("email", "luka@example.test") //$NON-NLS-1$ //$NON-NLS-2$
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claims(existing -> existing.putAll(claims))
                .build();
    }
}
