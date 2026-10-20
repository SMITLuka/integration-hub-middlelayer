package com.smit.integrationhubmiddlelayer.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides what an SSO user may do, from the "permissions" claim the SMIT SSO gateway puts into the
 * token. The gateway merges the feature flags of the user's roles for this app; SSO administrators
 * set them per role in the portal's roles tab ("Feature flags (JSON)").
 * <ul>
 * <li>Neither flag below: nothing. The gateway issues a token for this app to every signed-in SSO
 * user, also to users without any role for it, so this is the actual access check.</li>
 * <li>"is_app_user": read Mandators &amp; Companies and Interfaces &amp; Templates, read and write
 * Configurations &amp; Mappings.</li>
 * <li>"is_app_admin": read, write and delete on all three levels (no separate is_app_user needed).</li>
 * </ul>
 * Both count only as boolean true: the gateway merges non-boolean flags with "last role wins", so a flag
 * typed as the string "true" in one role must not grant anything. Users see every Mandator and Company;
 * the roles' data_scope (companies, departments) is not used. The token's own "is_admin" claim means
 * SSO portal administrator and is ignored here.
 */
@Component
public class PermissionPolicy
{
    static final String EMPLOYEE_AUTHORITY = "ROLE_" + SecurityConfig.EMPLOYEE_ROLE; //$NON-NLS-1$

    private static final String PERMISSIONS_CLAIM = "permissions"; //$NON-NLS-1$
    private static final String FEATURE_FLAGS = "feature_flags"; //$NON-NLS-1$
    private static final String APP_USER_FLAG = "is_app_user"; //$NON-NLS-1$
    private static final String APP_ADMIN_FLAG = "is_app_admin"; //$NON-NLS-1$

    private static final Map<AccessLevel, Set<AccessRight>> EMPLOYEE_RIGHTS = Map.of(
            AccessLevel.MANDATORS_COMPANIES, EnumSet.of(AccessRight.READ),
            AccessLevel.INTERFACES_TEMPLATES, EnumSet.of(AccessRight.READ),
            AccessLevel.CONFIGURATIONS_MAPPINGS, EnumSet.of(AccessRight.READ, AccessRight.WRITE));

    /**
     * The Spring authorities for this token: the employee role plus one authority per granted
     * level/right (see AccessLevel#authority). Empty for users without Integration Hub access in SSO.
     */
    public List<GrantedAuthority> authorities(Jwt jwt)
    {
        boolean admin = hasFlag(jwt, APP_ADMIN_FLAG);
        if (!admin && !hasFlag(jwt, APP_USER_FLAG))
        {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(EMPLOYEE_AUTHORITY));
        for (AccessLevel level : AccessLevel.values())
        {
            Set<AccessRight> rights = admin ? EnumSet.allOf(AccessRight.class) : EMPLOYEE_RIGHTS.getOrDefault(level, Set.of());
            rights.forEach(right -> authorities.add(new SimpleGrantedAuthority(level.authority(right))));
        }
        return authorities;
    }

    private static boolean hasFlag(Jwt jwt, String flag)
    {
        return jwt.getClaim(PERMISSIONS_CLAIM) instanceof Map<?, ?> permissions
                && permissions.get(FEATURE_FLAGS) instanceof Map<?, ?> featureFlags
                && Boolean.TRUE.equals(featureFlags.get(flag));
    }
}
