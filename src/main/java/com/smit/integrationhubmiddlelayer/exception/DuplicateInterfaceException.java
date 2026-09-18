package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when an Interface with the same name already exists.
 */
public class DuplicateInterfaceException extends ConflictException
{
    public DuplicateInterfaceException(String name)
    {
        super("DUPLICATE_INTERFACE", "Interface already exists with name: " + name); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
