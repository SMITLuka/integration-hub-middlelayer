package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Company's Mapping instance cannot be found by its id.
 */
public class CompanyMappingNotFoundException extends NotFoundException
{
    public CompanyMappingNotFoundException(Long companyMappingId)
    {
        super("COMPANY_MAPPING_NOT_FOUND", "Company mapping not found: " + companyMappingId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
