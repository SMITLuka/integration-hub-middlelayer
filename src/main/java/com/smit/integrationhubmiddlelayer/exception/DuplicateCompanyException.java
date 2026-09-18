package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Company with the same DMS company id already exists under the same Mandator.
 */
public class DuplicateCompanyException extends ConflictException
{
    public DuplicateCompanyException(Long mandatorId, String dmsCompanyId)
    {
        super("DUPLICATE_COMPANY", //$NON-NLS-1$
                "Company already exists with DMS company id " + dmsCompanyId + " under mandator " + mandatorId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
