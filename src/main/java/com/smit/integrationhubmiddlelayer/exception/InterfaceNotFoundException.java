package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when an Interface cannot be found by its id.
 */
public class InterfaceNotFoundException extends NotFoundException
{
    public InterfaceNotFoundException(Long interfaceId)
    {
        super("INTERFACE_NOT_FOUND", "Interface not found: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
