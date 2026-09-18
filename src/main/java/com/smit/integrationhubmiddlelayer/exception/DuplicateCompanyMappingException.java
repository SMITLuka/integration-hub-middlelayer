package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Company already has a Mapping instance for the given Interface.
 */
public class DuplicateCompanyMappingException extends ConflictException
{
    public DuplicateCompanyMappingException(Long companyId, Long interfaceId)
    {
        super("DUPLICATE_COMPANY_MAPPING", //$NON-NLS-1$
                "Company " + companyId + " already has a mapping for interface " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
