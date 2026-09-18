package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Mandator with the same external mandator id already exists.
 */
public class DuplicateMandatorException extends ConflictException
{
    public DuplicateMandatorException(String externalMandatorId)
    {
        super("DUPLICATE_MANDATOR", "Mandator already exists with external mandator id: " + externalMandatorId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
