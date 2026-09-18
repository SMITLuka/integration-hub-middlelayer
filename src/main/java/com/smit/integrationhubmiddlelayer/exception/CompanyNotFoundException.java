package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Company cannot be found by its id.
 */
public class CompanyNotFoundException extends NotFoundException
{
    public CompanyNotFoundException(Long companyId)
    {
        super("COMPANY_NOT_FOUND", "Company not found: " + companyId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
