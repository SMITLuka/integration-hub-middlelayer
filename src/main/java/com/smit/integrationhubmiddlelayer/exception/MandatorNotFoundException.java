package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Mandator cannot be found by its id.
 */
public class MandatorNotFoundException extends NotFoundException
{
    public MandatorNotFoundException(Long mandatorId)
    {
        super("MANDATOR_NOT_FOUND", "Mandator not found: " + mandatorId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
