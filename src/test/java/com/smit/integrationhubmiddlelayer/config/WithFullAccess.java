package com.smit.integrationhubmiddlelayer.config;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs a test as an employee with every right on every level (as a member of an admin workgroup),
 * for tests that exercise controller behaviour rather than permissions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@WithMockUser(authorities = { "ROLE_EMPLOYEE", //$NON-NLS-1$
        "MANDATORS_COMPANIES_READ", "MANDATORS_COMPANIES_WRITE", "MANDATORS_COMPANIES_DELETE", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "INTERFACES_TEMPLATES_READ", "INTERFACES_TEMPLATES_WRITE", "INTERFACES_TEMPLATES_DELETE", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        "CONFIGURATIONS_MAPPINGS_READ", "CONFIGURATIONS_MAPPINGS_WRITE", "CONFIGURATIONS_MAPPINGS_DELETE" }) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
public @interface WithFullAccess
{
}
