package com.smit.integrationhubmiddlelayer.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which authorities a token yields: none for non-employees, the base set for employees and every
 * right for members of an admin workgroup.
 */
class PermissionPolicyTest
{
    private final PermissionPolicy policy = new PermissionPolicy(" 45, 42 "); //$NON-NLS-1$

    @Test
    void extranetUser_getsNothing_evenInAnAdminWorkgroup()
    {
        assertThat(authorities(Map.of("bitrix_user_type", "extranet", "bitrix_workgroups", List.of("45")))).isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    @Test
    void employee_readsLevelsOneAndTwo_readsAndWritesLevelThree()
    {
        assertThat(authorities(Map.of("bitrix_user_type", "employee"))).containsExactlyInAnyOrder( //$NON-NLS-1$ //$NON-NLS-2$
                "ROLE_EMPLOYEE", //$NON-NLS-1$
                "MANDATORS_COMPANIES_READ", //$NON-NLS-1$
                "INTERFACES_TEMPLATES_READ", //$NON-NLS-1$
                "CONFIGURATIONS_MAPPINGS_READ", "CONFIGURATIONS_MAPPINGS_WRITE"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void employeeInOtherWorkgroups_keepsBaseRights()
    {
        assertThat(authorities(Map.of("bitrix_user_type", "employee", "bitrix_workgroups", List.of("7", "176")))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                .doesNotContain("MANDATORS_COMPANIES_WRITE", "CONFIGURATIONS_MAPPINGS_DELETE"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void adminWorkgroupMember_getsEveryRightOnEveryLevel()
    {
        List<String> authorities = authorities(Map.of("bitrix_user_type", "employee", "bitrix_workgroups", List.of("42"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        assertThat(authorities).hasSize(1 + AccessLevel.values().length * AccessRight.values().length);
        for (AccessLevel level : AccessLevel.values())
        {
            for (AccessRight right : AccessRight.values())
            {
                assertThat(authorities).contains(level.authority(right));
            }
        }
    }

    @Test
    void withoutConfiguredAdminWorkgroups_nobodyIsAdmin()
    {
        PermissionPolicy unconfigured = new PermissionPolicy(""); //$NON-NLS-1$
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("7") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                .issuedAt(now).expiresAt(now.plusSeconds(60))
                .claim("bitrix_user_type", "employee").claim("bitrix_workgroups", List.of("45", "42")).build(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        assertThat(unconfigured.authorities(jwt).stream().map(GrantedAuthority::getAuthority))
                .contains("CONFIGURATIONS_MAPPINGS_WRITE") //$NON-NLS-1$
                .noneMatch(authority -> authority.endsWith("_DELETE")); //$NON-NLS-1$
    }

    @Test
    void workgroupIdsMayArriveAsNumbers()
    {
        assertThat(authorities(Map.of("bitrix_user_type", "employee", "bitrix_workgroups", List.of(45)))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .contains("MANDATORS_COMPANIES_DELETE"); //$NON-NLS-1$
    }

    private List<String> authorities(Map<String, Object> claims)
    {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("7") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                .issuedAt(now).expiresAt(now.plusSeconds(60))
                .claims(existing -> existing.putAll(claims)).build();
        return policy.authorities(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }
}
