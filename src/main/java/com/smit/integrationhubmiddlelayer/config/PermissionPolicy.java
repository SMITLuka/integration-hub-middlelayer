package com.smit.integrationhubmiddlelayer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides what a logged-in Bitrix user may do, from the claims the Bitrix MCP server puts into the token.
 * <ul>
 * <li>Not an intranet employee (e.g. Bitrix extranet): nothing.</li>
 * <li>Every intranet employee: read Mandators &amp; Companies and Interfaces &amp; Templates,
 * read and write Configurations &amp; Mappings.</li>
 * <li>Members of an admin workgroup (integration-hub.auth.admin-workgroups / ADMIN_BITRIX_WORKGROUPS):
 * read, write and delete on all three levels. These must be closed (invite-only) Bitrix groups,
 * otherwise anyone could join one and make themselves admin; with none configured nobody is admin.</li>
 * </ul>
 */
@Component
public class PermissionPolicy
{
    static final String EMPLOYEE_AUTHORITY = "ROLE_" + SecurityConfig.EMPLOYEE_ROLE; //$NON-NLS-1$

    private static final String BITRIX_USER_TYPE_CLAIM = "bitrix_user_type"; //$NON-NLS-1$
    private static final String BITRIX_DEPARTMENTS_CLAIM = "bitrix_departments"; //$NON-NLS-1$
    private static final String BITRIX_WORKGROUPS_CLAIM = "bitrix_workgroups"; //$NON-NLS-1$

    private static final Map<AccessLevel, Set<AccessRight>> EMPLOYEE_RIGHTS = Map.of(
            AccessLevel.MANDATORS_COMPANIES, EnumSet.of(AccessRight.READ),
            AccessLevel.INTERFACES_TEMPLATES, EnumSet.of(AccessRight.READ),
            AccessLevel.CONFIGURATIONS_MAPPINGS, EnumSet.of(AccessRight.READ, AccessRight.WRITE));

    private final Set<String> adminWorkgroups;

    public PermissionPolicy(@Value("${integration-hub.auth.admin-workgroups:}") String adminWorkgroups) //$NON-NLS-1$
    {
        this.adminWorkgroups = Arrays.stream(adminWorkgroups.split(",")) //$NON-NLS-1$
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The Spring authorities for this token: the employee role plus one authority per granted
     * level/right (see AccessLevel#authority). Empty for users who are not intranet employees.
     */
    public List<GrantedAuthority> authorities(Jwt jwt)
    {
        if (!isIntranetEmployee(jwt))
        {
            return List.of();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(EMPLOYEE_AUTHORITY));
        boolean admin = isAdmin(jwt);
        for (AccessLevel level : AccessLevel.values())
        {
            Set<AccessRight> rights = admin ? EnumSet.allOf(AccessRight.class) : EMPLOYEE_RIGHTS.get(level);
            rights.forEach(right -> authorities.add(new SimpleGrantedAuthority(level.authority(right))));
        }
        return authorities;
    }

    private boolean isAdmin(Jwt jwt)
    {
        List<String> workgroups = jwt.getClaimAsStringList(BITRIX_WORKGROUPS_CLAIM);
        return workgroups != null && workgroups.stream().anyMatch(adminWorkgroups::contains);
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
