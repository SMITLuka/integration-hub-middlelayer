package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when attempting to delete a Mandator that still has Companies attached.
 * Companies must be removed explicitly first.
 */
public class MandatorHasCompaniesException extends ConflictException
{
    public MandatorHasCompaniesException(Long mandatorId)
    {
        super("MANDATOR_HAS_COMPANIES", "Mandator still has companies attached: " + mandatorId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
