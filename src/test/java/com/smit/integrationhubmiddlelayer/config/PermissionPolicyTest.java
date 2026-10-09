package com.smit.integrationhubmiddlelayer.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which authorities an SSO token yields: none without an Integration Hub flag, the base set for
 * is_app_user and every right for is_app_admin. The roles' companies play no part.
 */
class PermissionPolicyTest
{
    private final PermissionPolicy policy = new PermissionPolicy();

    @Test
    void appUser_readsLevelsOneAndTwo_readsAndWritesLevelThree()
    {
        assertThat(authorities(flags(Map.of("is_app_user", true)))).containsExactlyInAnyOrder( //$NON-NLS-1$
                "ROLE_EMPLOYEE", //$NON-NLS-1$
                "MANDATORS_COMPANIES_READ", //$NON-NLS-1$
                "INTERFACES_TEMPLATES_READ", //$NON-NLS-1$
                "CONFIGURATIONS_MAPPINGS_READ", "CONFIGURATIONS_MAPPINGS_WRITE"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void appAdmin_getsEveryRightOnEveryLevel_withoutAlsoBeingAppUser()
    {
        List<String> authorities = authorities(flags(Map.of("is_app_admin", true))); //$NON-NLS-1$

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
    void companiesWithoutFlag_grantNothing()
    {
        Map<String, Object> claims = Map.of("permissions", Map.of("feature_flags", Map.of(), //$NON-NLS-1$ //$NON-NLS-2$
                "data_scope", Map.of("companies", List.of("SMIT")))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertThat(authorities(claims)).isEmpty();
    }

    @Test
    void tokenWithoutUsablePermissions_getsNothing()
    {
        assertThat(authorities(Map.of())).isEmpty();
        assertThat(authorities(Map.of("permissions", Map.of()))).isEmpty(); //$NON-NLS-1$
        assertThat(authorities(Map.of("permissions", "not-an-object"))).isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(authorities(Map.of("permissions", Map.of("feature_flags", "is_app_user")))).isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Test
    void flagsMustBeBooleanTrue()
    {
        assertThat(authorities(flags(Map.of("is_app_user", "true")))).isEmpty(); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(authorities(flags(Map.of("is_app_user", false)))).isEmpty(); //$NON-NLS-1$
        assertThat(authorities(flags(Map.of("is_app_user", true, "is_app_admin", "true")))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                .noneMatch(authority -> authority.endsWith("_DELETE")); //$NON-NLS-1$
    }

    @Test
    void ssoPortalAdmin_isNeitherAppUserNorAppAdmin()
    {
        Map<String, Object> claims = new HashMap<>(flags(Map.of()));
        claims.put("is_admin", true); //$NON-NLS-1$

        assertThat(authorities(claims)).isEmpty();
    }

    private static Map<String, Object> flags(Map<String, Object> featureFlags)
    {
        return Map.of("permissions", Map.of("feature_flags", featureFlags, "data_scope", Map.of())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
