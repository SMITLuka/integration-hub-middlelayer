package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when attempting to create a Configuration Template for an Interface that already has one.
 */
public class DuplicateConfigurationTemplateException extends ConflictException
{
    public DuplicateConfigurationTemplateException(Long interfaceId)
    {
        super("DUPLICATE_CONFIGURATION_TEMPLATE", "Interface already has a configuration template: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
