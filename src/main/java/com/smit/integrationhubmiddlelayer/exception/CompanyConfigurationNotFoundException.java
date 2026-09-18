package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Company's Configuration instance cannot be found by its id.
 */
public class CompanyConfigurationNotFoundException extends NotFoundException
{
    public CompanyConfigurationNotFoundException(Long companyConfigurationId)
    {
        super("COMPANY_CONFIGURATION_NOT_FOUND", "Company configuration not found: " + companyConfigurationId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
