package com.smit.integrationhubmiddlelayer.config;

/**
 * The three permission levels of Integration Hub. Each level is granted read, write and delete
 * rights separately (see AccessRight); the resulting Spring authority is e.g. "MANDATORS_COMPANIES_WRITE".
 */
public enum AccessLevel
{
    /** Level 1: Mandators and their Companies (incl. their Additional Data). */
    MANDATORS_COMPANIES,
    /** Level 2: Interfaces with their Mapping and Configuration Templates. */
    INTERFACES_TEMPLATES,
    /** Level 3: a Company's Configurations and Mappings. */
    CONFIGURATIONS_MAPPINGS;

    public String authority(AccessRight right)
    {
        return name() + "_" + right.name(); //$NON-NLS-1$
    }
}
