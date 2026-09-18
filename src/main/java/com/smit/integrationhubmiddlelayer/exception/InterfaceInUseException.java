package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to delete an Interface that still has Company Mappings or Configurations referencing it.
 */
public class InterfaceInUseException extends ConflictException
{
    public InterfaceInUseException(Long interfaceId)
    {
        super("INTERFACE_IN_USE", "Interface is still in use by one or more companies: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
