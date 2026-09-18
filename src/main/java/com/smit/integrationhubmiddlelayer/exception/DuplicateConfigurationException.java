package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Company already has a Configuration instance for the given Interface.
 */
public class DuplicateConfigurationException extends ConflictException
{
    public DuplicateConfigurationException(Long companyId, Long interfaceId)
    {
        super("DUPLICATE_CONFIGURATION", //$NON-NLS-1$
                "Company " + companyId + " already has a configuration for interface " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
